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

package com.mw.beam.beamwallet.utxo

import com.mw.beam.beamwallet.base_screen.BaseRepository
import com.mw.beam.beamwallet.core.entities.OnTxStatusData
import com.mw.beam.beamwallet.core.entities.Utxo
import com.mw.beam.beamwallet.core.listeners.WalletListener
import io.reactivex.subjects.Subject

/**
 * Created by vain onnellinen on 10/2/18.
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class UtxoRepository : BaseRepository(), UtxoContract.Repository {

    override fun getUtxoUpdated(): Subject<List<Utxo>> {
        return getResult({ wallet?.getUtxosStatus() }, WalletListener.subOnAllUtxoChanged, object {}.javaClass.enclosingMethod.name)
    }

    override fun getTxStatus(): Subject<OnTxStatusData> {
        return getResult({ wallet?.getWalletStatus() }, WalletListener.subOnTxStatus, object {}.javaClass.enclosingMethod.name)
    }
}
