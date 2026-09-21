/**
 * 短信通知提效工具（PRD F-004）的通用逻辑。
 *
 * 定位：系统**不代付**短信费用，只负责「按模板拼话术 + 复制 / 唤起短信 App」，
 * 复用于部个人的手机短信能力。
 *
 * 模板变量（由 sys_config 里的模板定义）：
 *   {姓名} {系统链接} {初始密码} {拒绝原因}
 */

/** 模板变量占位符，如 {姓名} */
const VAR_PATTERN = /\{([^{}]+)\}/g

/**
 * 用变量替换模板里的 {xxx}。
 *
 * <p>未提供的变量**原样保留** —— 让干部一眼看出模板漏配变量，而不是静默留空。
 */
export function renderTemplate(template, vars = {}) {
  if (!template) {
    return ''
  }
  return template.replace(VAR_PATTERN, (match, rawName) => {
    const name = String(rawName).trim()
    return Object.prototype.hasOwnProperty.call(vars, name) && vars[name] != null
      ? String(vars[name])
      : match
  })
}

/** 手机号数组 → 分号分隔（短信 App 群发收件人格式） */
export function joinPhones(phones) {
  return (phones || []).filter(Boolean).join(';')
}

/** 是否 iOS —— sms: 协议的 body 分隔符不同 */
export function isIOS() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
}

/** 是否移动端浏览器 —— 只有移动端才值得提供 sms: 唤起 */
export function isMobileBrowser() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /Android|iPad|iPhone|iPod|Mobile/i.test(navigator.userAgent)
}

/**
 * 生成 sms: 链接（PRD F-004 移动端唤起）。
 *   Android: sms:号码?body=内容
 *   iOS:     sms:号码&body=内容
 */
export function buildSmsLink(phones, body) {
  const to = joinPhones(phones)
  if (!to) {
    return ''
  }
  const separator = isIOS() ? '&' : '?'
  return `sms:${to}${separator}body=${encodeURIComponent(body || '')}`
}

/**
 * 复制到剪贴板。
 *
 * <p>优先用异步剪贴板 API；不可用时（非安全上下文 / 旧内核）降级到 execCommand，
 * 避免干部在 http 环境下点了没反应。
 */
export async function copyText(text) {
  const value = text == null ? '' : String(text)
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(value)
      return true
    }
  } catch {
    // 落到下面的降级方案
  }
  try {
    const textarea = document.createElement('textarea')
    textarea.value = value
    textarea.setAttribute('readonly', '')
    textarea.style.position = 'fixed'
    textarea.style.top = '-1000px'
    document.body.appendChild(textarea)
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    return ok
  } catch {
    return false
  }
}
