/*
 * // Copyright 2018 Beam Development
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //    http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 */

package com.mw.beam.beamwallet.screens.settings

import com.mw.beam.beamwallet.BuildConfig
import com.mw.beam.beamwallet.base_screen.BasePresenter
import com.mw.beam.beamwallet.core.App
import com.mw.beam.beamwallet.core.AppConfig
import com.mw.beam.beamwallet.core.AppManager
import com.mw.beam.beamwallet.core.OnboardManager
import com.mw.beam.beamwallet.core.helpers.FingerprintManager
import com.mw.beam.beamwallet.core.helpers.QrHelper
import com.mw.beam.beamwallet.core.helpers.TrashManager
import com.mw.beam.beamwallet.core.helpers.TxStatus
import com.mw.beam.beamwallet.core.listeners.WalletListener
import io.reactivex.disposables.Disposable
import java.net.URI
import com.google.gson.Gson



/**
 *  1/21/19.
 */
class SettingsPresenter(currentView: SettingsContract.View, currentRepository: SettingsContract.Repository, private val state: SettingsState)
    : BasePresenter<SettingsContract.View, SettingsContract.Repository>(currentView, currentRepository),
        SettingsContract.Presenter {

    enum class ExportType {
        SAVE, SHARE
    }

    private lateinit var faucetGeneratedSubscription: Disposable
    private lateinit var exportDataSubscription: Disposable
    private var excludeExportParameters: Array<String> = arrayOf()

    private var exportType = ExportType.SAVE
    override fun onViewCreated() {
        super.onViewCreated()
        view?.init(repository.isEnabledConnectToRandomNode())
        view?.updateLockScreenValue(repository.getLockScreenValue())
        updateConfirmTransactionValue()
        updateFingerprintValue()
        view?.setAllowOpenExternalLinkValue(repository.isAllowOpenExternalLink())
        view?.setLogSettings(repository.getLogSettings())
    }


    override fun initSubscriptions() {
        super.initSubscriptions()

        faucetGeneratedSubscription = AppManager.instance.subOnFaucedGenerated.subscribe(){
            val link =  when (BuildConfig.FLAVOR) {
                AppConfig.FLAVOR_MAINNET -> "https://faucet.beamprivacy.community/?address=$it&type=mainnet"
                AppConfig.FLAVOR_TESTNET -> "https://faucet.beamprivacy.community/?address=$it&type=testnet"
                else -> "https://faucet.beamprivacy.community/?address=$it&type=masternet"
            }

            view?.onFaucetAddressGenerated(link)
        }

        exportDataSubscription = WalletListener.subOnDataExported.subscribe {
            val json = Gson()

            val allCategories = repository.getAllCategory()
            val categoriesJson = "{\"Categories\":" + json.toJson(allCategories) + ","
            val resultJson = categoriesJson + it.substring(1)

            val map = json.fromJson(resultJson, HashMap::class.java)

            for(param in excludeExportParameters){
                map.remove(param)
            }

            val result = json.toJson(map).toString()

            if (exportType == ExportType.SHARE) {
                val file = repository.getDataFile(result)
                view?.exportShare(file)
            }
            else{
                view?.exportSave(result)
            }
        }
    }

    override fun omImportPressed() {
        view?.showImportDialog()
    }

    override fun onStart() {
        super.onStart()
        view?.updateCategoryList(repository.getAllCategory())
        view?.setLanguage(repository.getCurrentLanguage())
    }

    override fun onAddCategoryPressed() {
        view?.navigateToAddCategory()
    }

    override fun onCategoryPressed(categoryId: String) {
        view?.navigateToCategory(categoryId)
    }


    private fun updateConfirmTransactionValue() {
        view?.updateConfirmTransactionValue(repository.shouldConfirmTransaction())
    }

    private fun updateFingerprintValue() {
        if (FingerprintManager.SensorState.READY == FingerprintManager.checkSensorState(view?.getContext()
                        ?: return)) {
            view?.showFingerprintSettings(repository.isFingerPrintEnabled())
        } else {
            repository.saveEnableFingerprintSettings(false)
        }
    }

    override fun onReportProblem() {
        view?.sendMailWithLogs()
    }

    override fun onChangePass() {
        view?.changePass()
    }

    override fun onShowOwnerKey() {
        view?.navigateToOwnerKeyVerification()
    }

    override fun onSeedPressed() {
        view?.navigateToSeed()
    }

    override fun hasBackArrow(): Boolean? = true
    override fun hasStatus(): Boolean = true

    override fun onShowLockScreenSettings() {
        view?.showLockScreenSettingsDialog()
    }

    override fun onChangeLockSettings(millis: Long) {
        repository.saveLockSettings(millis)

        view?.apply {
            updateLockScreenValue(repository.getLockScreenValue())
            closeDialog()
        }
    }

    override fun onChangeConfirmTransactionSettings(isConfirm: Boolean) {
        if (isConfirm) {
            repository.saveConfirmTransactionSettings(isConfirm)
        } else {
            view?.showConfirmPasswordDialog({
                repository.saveConfirmTransactionSettings(isConfirm)
            }, ::updateConfirmTransactionValue)
        }
    }

    override fun onChangeFingerprintSettings(isEnabled: Boolean) {
        if (isEnabled) {
            repository.saveEnableFingerprintSettings(isEnabled)
        } else {
            view?.showConfirmPasswordDialog({
                repository.saveEnableFingerprintSettings(isEnabled)
            }, ::updateFingerprintValue)
        }
    }

    override fun onChangeAllowOpenExternalLink(allowOpen: Boolean) {
        repository.setAllowOpenExternalLink(allowOpen)
    }

    override fun onChangeLogSettings(days: Long) {
        var d = when(days) {
            0L->5L
            1L->15L
            2L->30L
            else -> 0L
        }
        repository.saveLogSettings(d)

        view?.setLogSettings(repository.getLogSettings())

        App.self.clearLogs()
    }

    override fun onChangeNodeAddress() {
        view?.clearInvalidNodeAddressError()
    }

    override fun onNodeAddressPressed() {
        if (!repository.isEnabledConnectToRandomNode()) {
            view?.showNodeAddressDialog(repository.getCurrentNodeAddress())
        }
    }

    override fun onLanguagePressed() {
        view?.navigateToLanguage()
    }

    override fun onClearDataPressed() {
        view?.showClearDataDialog()
    }

    override fun onLogsPressed() {
        view?.showLogsDialog()
    }

    override fun onDialogClearDataPressed(clearAddresses: Boolean, clearContacts: Boolean, clearTransactions: Boolean) {
        view?.closeDialog()
        if (clearAddresses || clearContacts || clearTransactions) {
            view?.showClearDataAlert(clearAddresses, clearContacts, clearTransactions)
        }
    }

    override fun onConfirmClearDataPressed(clearAddresses: Boolean, clearContacts: Boolean, clearTransactions: Boolean) {
        if (clearAddresses) {
            state.addresses.forEach { repository.deleteAddress(it.walletID) }
        }

        if (clearContacts) {
            state.contacts.forEach { repository.deleteAddress(it.walletID) }
        }

        if (clearTransactions) {
            state.transactions.forEach {
                repository.deleteTransaction(it)
            }
        }
    }

    override fun onChangeRunOnRandomNode(isEnabled: Boolean) {
        if (isEnabled == repository.isEnabledConnectToRandomNode()) {
            return
        }

        if (isEnabled) {
            repository.setRunOnRandomNode(isEnabled)
            view?.init(isEnabled)
            return
        }

        val savedAddress = repository.getSavedNodeAddress()

        if (!savedAddress.isNullOrBlank() && isValidNodeAddress(savedAddress)) {
            repository.setNodeAddress(savedAddress)
            repository.setRunOnRandomNode(isEnabled)
            view?.init(isEnabled)
        } else {
            view?.init(true)
            view?.showNodeAddressDialog(repository.getCurrentNodeAddress())
        }
    }

    override fun onSaveNodeAddress(address: String?) {
        if (!address.isNullOrBlank() && isValidNodeAddress(address)) {
            view?.closeDialog()
            repository.setNodeAddress(address)
            repository.setRunOnRandomNode(false)
            view?.init(false)
        } else {
            view?.showInvalidNodeAddressError()
        }
    }

    private fun isValidNodeAddress(address: String): Boolean {
        return try {
            val uri = URI(QrHelper.BEAM_URI_PREFIX + address)
            val isValidPort = if (uri.port == -1) { true } else { uri.port in 1..65535 }

            !uri.host.isNullOrBlank() && address.last() != ':' && isValidPort
        } catch (e: Exception) {
            false
        }
    }

    override fun onReceiveFaucet() {
        view?.showReceiveFaucet()
    }

    override fun onProofPressed() {
        view?.navigateToPaymentProof()
    }

    override fun generateFaucetAddress() {
        AppManager.instance.createAddressForFaucet()
    }

    override fun onDialogClosePressed() {
        view?.closeDialog()
    }

    override fun onExportPressed() {
        view?.showExportDialog()
    }

    override fun onExportWithExclude(list: Array<String>) {
        excludeExportParameters = list
        view?.showExportSaveDialog()
    }

    override fun onExportSave() {
        exportType = ExportType.SAVE
        AppManager.instance.wallet?.exportDataToJson()
    }

    override fun onExportShare() {
        exportType = ExportType.SHARE
        AppManager.instance.wallet?.exportDataToJson()
    }

    override fun onRemoveWalletPressed() {
        view?.showConfirmRemoveWallet()
    }

    override fun onConfirmRemoveWallet() {
        AppManager.instance.removeWallet()
        view?.walletRemoved()
    }

    override fun onDestroy() {
        view?.closeDialog()
        super.onDestroy()
    }

    override fun getSubscriptions(): Array<Disposable>? = arrayOf(faucetGeneratedSubscription,exportDataSubscription)
}
