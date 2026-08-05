package com.futo.music.states

import com.futo.futopay.PaymentState
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.StringStorage

const val isTestingPayment = false;

class StatePayment: PaymentState(if(isTestingPayment) VERIFICATION_PUBLIC_KEY_TESTING else VERIFICATION_PUBLIC_KEY) {

    public override val isTesting: Boolean get() = isTestingPayment;

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
        private val VERIFICATION_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzhYbz1w0eezegXZJz+Ho\n" +
                "360EaBQRlkkDxDMDHYfLuk5zHMMJH/7OdS6PutBSdqkfmXQ2F0OF7+R4zEl8fhSB" +
                "TvIwqW1209lDiRo6i8khAypZoGeesbU414hWGP5Bf7S07BwjnDT+6OTBztM8xT86" +
                "Z6n5b7tlVh0So+UL0K3F217ivB8epp78fNszlhsd/w4HcEpN5Rj7YpGy1+nEkasa" +
                "JOINoyDrWGu/a8SogTKio5oEmG4uiZ1+y5NIsKTlsmjaVlb0Nodoc6xETBr5d4mn" +
                "oUdP+qY57/Q+w6N/Cvthn+EZHOqNCyPFVaDjg0bFc2gDyK1a+zZqiv9IJFxqw81G" +
                "JwIDAQAB";

        private val VERIFICATION_PUBLIC_KEY_TESTING = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmuvxAcKFKzhtbAFEJXal" +
                "dTXO99WohJ3ZZGYqQQvSWsIYbvSWQC/jNuM7uG6T3SFoQ7LnHYn7xPhOZJY+A+u9" +
                "RuE3ZBVSsRUrZUgziNRu1OEQfhrxFkreDuWKG1GMIR20vAwMRZUpmkbMm2RYDqXE" +
                "ho6ur0gDXXPYqLlhuTS66v5BIke65H4/ddfg38XaeQQCVx/sP5T4P2OAWt96yaeF" +
                "vBmv4dfQsLQTCE1pwuokh7lje70ayDnKg3r1t8OBLhf5rEfTfg2Tv9HIDpX1X/Rr" +
                "IyvNVQg3D9ej0cGnrJ+kGEr9K3vO9p26rKXzF3DcJ65llaTBWEMalRe722paypTj" +
                "ZQIDAQAB";

        /*
        private val VERIFICATION_PUBLIC_KEY_TESTING = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyXSsZPdXhWFh0UmV6fmG\n" +
                "CVtDv9YBbFdiNqRYX04yDbwDUb41amLaZpLx32Cv8+57LVILE7kcKNMWoWSFmuBm\n" +
                "GJ/TTmUhG6vmS5bde1eFPMDWAsyLaV78YHXne9bi4K5fgeuCw8RnD1xEBzkYA3lR\n" +
                "NpYrykRewttj+A3qZJB7jBCLrmpWpRLo1h9LsPpp10dTuYIF6M6XCJ/s8uOZdGGd\n" +
                "um4VBKdhcQFoBp8zXbr8biFa4jR9EoPLzA9u1fwxvsA4aUJEajHTfpj0uO2gJ1Kl\n" +
                "4vU2499H6J3qdn6Fvz8HralRlTaLN7CNw1FEnqxCidJww2iYO5X7EYME+g8fTMGO\n" +
                "/wIDAQAB";
        */

        private var _instance : StatePayment? = null;
        val instance : StatePayment
            get(){
                if(_instance == null)
                    _instance = StatePayment();
                return _instance!!;
            };
    }
}