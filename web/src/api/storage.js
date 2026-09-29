import request from './request'

/**
 * 存储维护（T26，仅超管）。
 *
 * 刻意分成「扫描」与「清理」两步调用：
 *   1. scanStorageOrphans() 是 **dry-run** —— 只读不删，返回孤儿清单
 *   2. cleanStorageOrphans(keys) 必须**显式传 key** —— 服务端还会逐个复检「是否仍是孤儿」，
 *      不是就跳过。这样「用户看到的清单」与「被删的东西」是同一份，不会误删。
 */

/** 扫描孤儿文件（只读不删）。返回 { bucket, totalObjects, referencedObjects, orphanBytes, truncated, orphans } */
export const scanStorageOrphans = () => request.get('/storage/admin/orphans')

/** 清理指定的孤儿对象（key 来自上一次扫描结果）。返回 { deleted, skipped, failed } */
export const cleanStorageOrphans = (keys) => request.post('/storage/admin/orphans/clean', { keys })
