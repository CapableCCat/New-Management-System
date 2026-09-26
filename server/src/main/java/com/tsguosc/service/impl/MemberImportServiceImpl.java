package com.tsguosc.service.impl;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.ImportAccountVO;
import com.tsguosc.dto.ImportErrorVO;
import com.tsguosc.dto.ImportResultVO;
import com.tsguosc.dto.ImportRow;
import com.tsguosc.entity.SysDict;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.MemberImportService;
import com.tsguosc.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel 批量导入实现（PRD F-009）。
 *
 * <p>四个关键取舍：
 * <ol>
 *   <li><b>读取用原始行（{@code Map<Integer,String>}）而不是注解映射</b>：错误清单要精确到**行号**，
 *       自己逐行遍历才控得住；注解映射一旦列顺序变了会静默错位。</li>
 *   <li><b>列头先校验、不合就整表拒绝</b>（PRD：提示「请使用标准模板」），避免"列错位了还在闷头建号"。</li>
 *   <li><b>逐行独立</b>：单行失败只记录原因、不影响其它行；整体**不加事务**（与 T7 批量通过同口径）。</li>
 *   <li><b>字典一律按 label 反查 code</b>：Excel 里人填的是中文名称，落库要的是 code；
 *       反查前先把字典启用项一次性load 进内存，别逐行查库。</li>
 * </ol>
 *
 * <p>与旧系统（{@code ExcelServiceImpl}）的差别正是这几点 —— 旧实现遇第一个错行就抛异常中断整表、
 * 没有行号、学号当必填、密码写死明文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberImportServiceImpl implements MemberImportService {

    /** 模板列头，顺序即校验顺序（PRD F-009） */
    private static final List<String> HEADERS = List.of(
            ImportRow.COL_NAME, ImportRow.COL_PHONE, ImportRow.COL_STUDENT_ID,
            ImportRow.COL_COLLEGE, ImportRow.COL_MAJOR, ImportRow.COL_DEPARTMENT, ImportRow.COL_DUTY);

    /** Excel 第 1 行是列头，明细从第 2 行开始 */
    private static final int HEADER_ROW_NUMBER = 1;

    /** 单次导入行数上限：一次性导入不会超过社团队伍规模，设个闸门避免超大文件拖垮服务 */
    private static final int MAX_ROWS = 5000;

    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_STUDENT_ID_LENGTH = 32;

    /** 专业字典里没有的，按「其他 + 原文」兜底（§6 D5） */
    private static final String CODE_OTHER = "other";

    private static final String SHEET_MEMBERS = "成员名单";
    private static final String SHEET_HELP = "填写说明";

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserMapper userMapper;
    private final SysDictMapper sysDictMapper;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------
    // 模板
    // ------------------------------------------------------------

    @Override
    public byte[] buildTemplate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(out).build()) {
            // sheet 1：只有列头（列名由 ImportRow 的注解生成，与校验用的是同一份常量）
            WriteSheet memberSheet = EasyExcel.writerSheet(0, SHEET_MEMBERS).head(ImportRow.class).build();
            writer.write(Collections.emptyList(), memberSheet);

            // sheet 2：填写说明
            WriteSheet helpSheet = EasyExcel.writerSheet(1, SHEET_HELP).head(helpHead()).build();
            writer.write(helpRows(), helpSheet);
        }
        return out.toByteArray();
    }

    private List<List<String>> helpHead() {
        return List.of(List.of("列名"), List.of("是否必填"), List.of("填写说明"));
    }

    private List<List<String>> helpRows() {
        return List.of(
                List.of(ImportRow.COL_NAME, "必填", "姓名，最长 32 个字"),
                List.of(ImportRow.COL_PHONE, "必填", "11 位手机号，同时是登录账号；库里已有的手机号会跳过并列入结果"),
                List.of(ImportRow.COL_STUDENT_ID, "可空", "学号；填了就必须唯一，最长 32 个字。留空表示尚未补录"),
                List.of(ImportRow.COL_COLLEGE, "必填", "学院名称，需与「字典管理 → 学院」里的一致"),
                List.of(ImportRow.COL_MAJOR, "必填", "专业名称；字典里没有的会按「其他 + 原文」保存"),
                List.of(ImportRow.COL_DEPARTMENT, "必填", "部门名称，需与字典一致（如 技术部）"),
                List.of(ImportRow.COL_DUTY, "必填", "职位名称，需与字典一致（如 成员）"),
                List.of("提示", "—", "第 1 行是列头请勿修改；数据从第 2 行开始填；初始密码只在导入结果页显示一次"));
    }

    // ------------------------------------------------------------
    // 导入
    // ------------------------------------------------------------

    @Override
    public ImportResultVO importMembers(MultipartFile file) {
        assertExcelFile(file);
        List<Map<Integer, String>> rows = readRows(file);
        if (rows.isEmpty()) {
            throw err("文件里没有内容，请使用标准模板填写后重试");
        }
        assertHeader(rows.get(0));

        List<Map<Integer, String>> dataRows = rows.subList(1, rows.size());
        List<Map<Integer, String>> filledRows = dataRows.stream().filter(row -> !isBlankRow(row)).toList();
        if (filledRows.isEmpty()) {
            throw err("文件里没有可导入的数据行，请从第 2 行开始填写");
        }
        if (filledRows.size() > MAX_ROWS) {
            throw err("单次最多导入 " + MAX_ROWS + " 行，请拆分文件后分批导入");
        }

        Map<String, Map<String, String>> dictIndex = loadDictIndex();
        Set<String> phoneTaken = existingValues(User::getPhone);
        Set<String> studentIdTaken = existingValues(User::getStudentId);
        Set<String> phoneInFile = new HashSet<>();
        Set<String> studentIdInFile = new HashSet<>();

        List<ImportAccountVO> accounts = new ArrayList<>();
        List<ImportErrorVO> errors = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i++) {
            Map<Integer, String> cells = dataRows.get(i);
            if (isBlankRow(cells)) {
                continue;
            }
            int excelRow = i + HEADER_ROW_NUMBER + 1;
            try {
                PendingAccount pending = buildAccount(cells, dictIndex, phoneTaken, studentIdTaken, phoneInFile, studentIdInFile);
                userMapper.insert(pending.user());
                phoneTaken.add(pending.user().getPhone());
                if (pending.user().getStudentId() != null) {
                    studentIdTaken.add(pending.user().getStudentId());
                }
                accounts.add(new ImportAccountVO(pending.user().getName(), pending.user().getPhone(), pending.password()));
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(excelRow, e.getMessage()));
            } catch (Exception e) {
                log.error("Excel 导入单行失败：row={}", excelRow, e);
                errors.add(new ImportErrorVO(excelRow, "该行保存失败，请检查内容后重试"));
            }
        }

        log.info("Excel 导入完成：明细 {} 行，成功建号 {}，跳过 {}", filledRows.size(), accounts.size(), errors.size());
        return new ImportResultVO(filledRows.size(), accounts.size(), errors.size(), accounts, errors);
    }

    /** 一行通过校验后待落库的账号（明文密码只活到本次响应） */
    private record PendingAccount(User user, String password) {
    }

    private PendingAccount buildAccount(Map<Integer, String> cells,
                                        Map<String, Map<String, String>> dictIndex,
                                        Set<String> phoneTaken, Set<String> studentIdTaken,
                                        Set<String> phoneInFile, Set<String> studentIdInFile) {
        String name = cell(cells, 0);
        String phone = cell(cells, 1);
        String studentId = cell(cells, 2);
        String collegeLabel = cell(cells, 3);
        String majorLabel = cell(cells, 4);
        String departmentLabel = cell(cells, 5);
        String dutyLabel = cell(cells, 6);

        if (name == null) {
            throw err("姓名为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw err("姓名超过 " + MAX_NAME_LENGTH + " 个字");
        }
        if (phone == null) {
            throw err("手机号为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw err("手机号格式不正确");
        }
        // 先判「文件内重复」再判「库里已存在」：重跑同一份文件时，手机号既在库里、也在文件里各出现一次，
        // 此时按库内已存在报更贴切；而同一文件里写了两遍的，就应该报文件内重复（实测踩到）
        if (!phoneInFile.add(phone)) {
            throw err("文件内手机号重复");
        }
        if (phoneTaken.contains(phone)) {
            throw err("该手机号已存在账号");
        }
        if (studentId != null) {
            if (studentId.length() > MAX_STUDENT_ID_LENGTH) {
                throw err("学号超过 " + MAX_STUDENT_ID_LENGTH + " 个字");
            }
            if (studentIdTaken.contains(studentId)) {
                throw err("该学号已被使用");
            }
            if (!studentIdInFile.add(studentId)) {
                throw err("文件内学号重复");
            }
        }

        String college = requireDictCode(dictIndex, DictType.COLLEGE, collegeLabel, "学院");
        String department = requireDictCode(dictIndex, DictType.DEPARTMENT, departmentLabel, "部门");
        String duty = requireDictCode(dictIndex, DictType.DUTY, dutyLabel, "职位");

        // 专业：字典里没有就按「其他 + 原文」兜底（学院/部门/职位不兜底，填错就是错）
        if (majorLabel == null) {
            throw err("专业为空");
        }
        String major = dictCode(dictIndex, DictType.MAJOR, majorLabel);
        String majorText = null;
        if (major == null) {
            major = CODE_OTHER;
            majorText = majorLabel;
        }

        User user = new User();
        user.setName(name);
        user.setPhone(phone);
        // 学号空串已在 cell() 归一为 null；MySQL 唯一索引允许多个 NULL（§6 D72）
        user.setStudentId(studentId);
        user.setCollege(college);
        user.setMajor(major);
        user.setMajorText(majorText);
        user.setDepartment(parseDictCode(department, "部门"));
        user.setDuty(parseDictCode(duty, "职位"));
        user.setRole(0);
        user.setStatus(0);
        // 模板里没有性别列，按「未填」入库（§6 D6）
        user.setGender(0);
        // 导入建号与审核通过同一口径：随机初始密码 + activated_at=NULL 触发首登强制改密（§6 D35）
        String password = PasswordGenerator.random(8);
        user.setPassword(passwordEncoder.encode(password));
        user.setActivatedAt(null);
        return new PendingAccount(user, password);
    }

    // ------------------------------------------------------------
    // 读取与校验
    // ------------------------------------------------------------

    private void assertExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw err("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw err("只支持 .xlsx / .xls 格式的 Excel 文件");
        }
        // 再看文件头：扩展名是用户随手改的，内容才是判据（与图片三层校验同一思路，§6 D79）
        if (!looksLikeExcel(file)) {
            throw err("文件内容不是有效的 Excel，请用模板另存为 .xlsx 后再上传");
        }
    }

    /**
     * 文件头魔数：xlsx 本质是 zip（PK\x03\x04）；老式 xls 是 OLE2 复合文档。
     *
     * <p>不加这道校验的话，随便一个二进制文件喂给 Excel 库会被它当**文本/CSV** 兜底解析，
     * 于是报出「请使用标准模板」——把「文件坏了」错说成「列头不对」，误导用户（实测踩到）。
     */
    private boolean looksLikeExcel(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(8);
        } catch (IOException e) {
            throw err("读取上传文件失败，请重试");
        }
        // xlsx：zip 头 PK\x03\x04
        if (head.length >= 4 && head[0] == 0x50 && head[1] == 0x4B) {
            return true;
        }
        // 老式 xls：OLE2 复合文档头
        byte[] ole = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        if (head.length >= ole.length) {
            for (int i = 0; i < ole.length; i++) {
                if (head[i] != ole[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 读出所有行（含列头行）。
     *
     * <p>{@code headRowNumber(0)}：不把任何行当表头，于是列头也作为普通行返回，列头校验自己来做。
     */
    private List<Map<Integer, String>> readRows(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            List<Map<Integer, String>> rows = EasyExcel.read(in).sheet(0).headRowNumber(0).doReadSync();
            return rows == null ? List.of() : rows;
        } catch (IOException e) {
            throw err("读取上传文件失败，请重试");
        } catch (Exception e) {
            log.warn("Excel 解析失败：{}", e.getMessage());
            throw err("文件解析失败，请确认是标准模板导出的 .xlsx / .xls 文件");
        }
    }

    /** 列头必须与模板完全一致（含列数与顺序），否则整表拒绝 */
    private void assertHeader(Map<Integer, String> header) {
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < HEADERS.size(); i++) {
            actual.add(normalizeCell(header.get(i)));
        }
        if (!HEADERS.equals(actual)) {
            throw err("请使用标准模板（第 1 行列头应为：" + String.join("、", HEADERS) + "）");
        }
        // 模板只有 7 列：右侧多出内容说明不是标准模板
        for (int i = HEADERS.size(); i < HEADERS.size() + 10; i++) {
            if (normalizeCell(header.get(i)) != null) {
                throw err("请使用标准模板（列数应为 " + HEADERS.size() + " 列：" + String.join("、", HEADERS) + "）");
            }
        }
    }

    private String cell(Map<Integer, String> cells, int index) {
        return normalizeCell(cells.get(index));
    }

    /**
     * 单元格归一：去空白与不可见字符；**把 Excel 读出来的科学计数法还原成整数串**。
     *
     * <p>最后一件事很关键：手机号列如果被 Excel 当成数字，读出来可能是 {@code 1.39E+10}
     * 或 {@code 13900000000.0}，不还原就会整列判成"手机号格式不正确"。
     */
    private String normalizeCell(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replace("\uFEFF", "").replace('\u00A0', ' ').trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches("^\\d+(\\.\\d+)?[eE][+-]?\\d+$") || value.matches("^\\d+\\.0+$")) {
            try {
                return new BigDecimal(value).toPlainString();
            } catch (NumberFormatException ignored) {
                // 不是数字就当普通文本处理
            }
        }
        return value;
    }

    private boolean isBlankRow(Map<Integer, String> cells) {
        return cells == null || cells.values().stream().allMatch(value -> normalizeCell(value) == null);
    }

    // ------------------------------------------------------------
    // 字典与库内唯一性
    // ------------------------------------------------------------

    /** 字典索引：type → (label → code)，只含启用项；导入前查一次 */
    private Map<String, Map<String, String>> loadDictIndex() {
        List<SysDict> list = sysDictMapper.selectList(
                Wrappers.<SysDict>lambdaQuery().eq(SysDict::getEnabled, 1));
        Map<String, Map<String, String>> index = new HashMap<>();
        for (SysDict dict : list) {
            if (dict.getLabel() == null || dict.getCode() == null) {
                continue;
            }
            index.computeIfAbsent(dict.getType(), key -> new HashMap<>())
                    .put(dict.getLabel().trim(), dict.getCode().trim());
        }
        return index;
    }

    private String dictCode(Map<String, Map<String, String>> index, DictType type, String label) {
        if (label == null) {
            return null;
        }
        Map<String, String> labels = index.get(type.getCode());
        return labels == null ? null : labels.get(label);
    }

    private String requireDictCode(Map<String, Map<String, String>> index, DictType type,
                                   String label, String columnName) {
        if (label == null) {
            throw err(columnName + "为空");
        }
        String code = dictCode(index, type, label);
        if (code == null) {
            throw err(columnName + "「" + label + "」不存在，请先在「字典管理」里确认");
        }
        return code;
    }

    private int parseDictCode(String code, String columnName) {
        try {
            return Integer.parseInt(code.trim());
        } catch (NumberFormatException e) {
            throw err(columnName + "字典编码不是数字，请检查字典配置");
        }
    }

    /** 已有值集合（手机号 / 学号），一次查库覆盖整批，避免逐行 exists */
    private Set<String> existingValues(Function<User, String> column) {
        return userMapper.selectList(Wrappers.<User>lambdaQuery().select(User::getPhone, User::getStudentId))
                .stream()
                .map(column)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private BusinessException err(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
