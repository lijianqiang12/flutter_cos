package com.shuge888.flutter_cos

import com.tencent.qcloud.core.auth.BasicLifecycleCredentialProvider

/**
 * 方法二：使用临时密钥进行签名（推荐使用这种方法），此处假设已获取了临时密钥 tempSecretKey, tempSecretId,
 * sessionToken, expiredTime.
 */
class LocalSessionCredentialProvider(
    private val tempSecretId: String,
    private val tempSecretKey: String,
    private val sessionToken: String,
    private val expiredTime: Long
) : BasicLifecycleCredentialProvider() {
    /**
     * 返回 SessionQCloudCredential
     */
    @Throws(QCloudClientException::class)
    protected fun fetchNewCredentials(): QCloudLifecycleCredentials {
        return SessionQCloudCredentials(tempSecretId, tempSecretKey, sessionToken, expiredTime)
    }
}