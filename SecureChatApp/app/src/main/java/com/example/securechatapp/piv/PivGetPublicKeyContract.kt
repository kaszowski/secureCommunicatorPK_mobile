package com.example.securechatapp.piv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.yubico.yubikit.android.ui.YubiKeyPromptAction
import com.yubico.yubikit.android.ui.YubiKeyPromptActivity
import com.yubico.yubikit.android.ui.YubiKeyPromptConnectionAction
import com.yubico.yubikit.core.application.CommandState
import com.yubico.yubikit.core.smartcard.SmartCardConnection
import com.yubico.yubikit.core.util.Pair
import com.yubico.yubikit.piv.PivSession
import com.yubico.yubikit.piv.Slot
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

private  const val EXTRA_SLOT = "SLOT"
private  const val EXTRA_PUBLIC_KEY = "PUBLIC_KEY"

class PivGetPublicKeyContract : ActivityResultContract<Slot, PublicKey?>() {
    override fun createIntent(context: Context, input: Slot): Intent = YubiKeyPromptActivity.createIntent(context, PivGetPublicKeyAction::class.java).apply{
        putExtra(EXTRA_SLOT, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PublicKey? {
        return when(resultCode){
            Activity.RESULT_OK -> {
                val bytes = intent!!.getByteArrayExtra(EXTRA_PUBLIC_KEY)
                val kf = KeyFactory.getInstance("RSA")
                val spec = X509EncodedKeySpec(bytes)
                kf.generatePublic(spec)
            }
            else -> null
        }
    }

}

@Suppress("DEPRECATION")
class PivGetPublicKeyAction : YubiKeyPromptConnectionAction<SmartCardConnection>(SmartCardConnection::class.java){
    override fun onYubiKeyConnection(
        connection: SmartCardConnection,
        extras: Bundle,
        commandState: CommandState?
    ): Pair<Int, Intent>? {
        val slot = extras.getSerializable(EXTRA_SLOT) as Slot
        val piv = PivSession(connection)
        val publicKey =  piv.getCertificate(slot).publicKey

        return Pair(Activity.RESULT_OK, Intent().apply(){
            putExtra(EXTRA_PUBLIC_KEY, publicKey.encoded)
        })
    }

}
