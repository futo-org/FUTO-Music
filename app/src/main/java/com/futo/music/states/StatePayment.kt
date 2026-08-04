package com.futo.music.states

import com.futo.futopay.PaymentState
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.StringStorage

const val isTestingPayment = true;

class StatePayment: PaymentState(if(isTestingPayment) VERIFICATION_PUBLIC_KEY_TESTING else VERIFICATION_PUBLIC_KEY) {

    override val isTesting: Boolean get() = isTestingPayment;

    override val polarOrgSlug: String get() = "futo-music";
    override val polarProductSlug: String get() = "futo-music";
    override val polarProductId: String get() = "futo-music";
    override val appActivatePrefix: String get() = "futo-music";

    override fun savePaymentKey(licenseKey: String, licenseActivation: String) {
        FragmentedStorage.get<StringStorage>("paymentLicenseKey").setAndSave(licenseKey);
        FragmentedStorage.get<StringStorage>("paymentLicenseActivation").setAndSave(licenseActivation);
    }

    override fun getPaymentKey(): Pair<String, String> {
        return Pair(FragmentedStorage.get<StringStorage>("paymentLicenseKey").value, FragmentedStorage.get<StringStorage>("paymentLicenseActivation").value);
    }


    companion object {
        private val VERIFICATION_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvdPgWITZZt9l3E2XGWKb" +
                "53r35qPNG3zh0xPi1oHzTMzwLMcz/OPo91C5Gih0kp7L+EgQg9i4lDFjFlIPFSGT" +
                "I/uCr2jmoK/EZhdV6VSojQCudvkrnu08ErNzmkHoHEdKnW5ab7vy0vwydLyVGnq+" +
                "sdkcmh1LNQdj5mJzMaVrlFE+s3gm/ES+HVA/9leulIA9dlv+rnD3dE9pYdRN68qw" +
                "TOpL5uwO4zHBpZOjYfZ7hKRLUfkeyeAiaUUt6kYmRLdeO6v1B+ntB1Qmte7SBpI2" +
                "BgYGkR2uUDpU/BWdnAtynd7av14QoteKwVyPpTuwddmJKZOMUdaf+SYwfNIWWOne" +
                "YQIDAQAB";
        private val VERIFICATION_PUBLIC_KEY_TESTING = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyXSsZPdXhWFh0UmV6fmG\n" +
                "CVtDv9YBbFdiNqRYX04yDbwDUb41amLaZpLx32Cv8+57LVILE7kcKNMWoWSFmuBm\n" +
                "GJ/TTmUhG6vmS5bde1eFPMDWAsyLaV78YHXne9bi4K5fgeuCw8RnD1xEBzkYA3lR\n" +
                "NpYrykRewttj+A3qZJB7jBCLrmpWpRLo1h9LsPpp10dTuYIF6M6XCJ/s8uOZdGGd\n" +
                "um4VBKdhcQFoBp8zXbr8biFa4jR9EoPLzA9u1fwxvsA4aUJEajHTfpj0uO2gJ1Kl\n" +
                "4vU2499H6J3qdn6Fvz8HralRlTaLN7CNw1FEnqxCidJww2iYO5X7EYME+g8fTMGO\n" +
                "/wIDAQAB";
        private var _instance : StatePayment? = null;
        val instance : StatePayment
            get(){
                if(_instance == null)
                    _instance = StatePayment();
                return _instance!!;
            };
    }
}