package com.example.melody.data.session

import javax.crypto.SecretKey

interface AuthSessionKeyProvider {
    fun getOrCreateKey(): SecretKey

    fun getExistingKey(): SecretKey?
}
