package com.shary.app.infrastructure.services.connectivity

import com.shary.app.core.domain.interfaces.ports.CloudStatusDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NoopCloudStatusDataSource @Inject constructor() : CloudStatusDataSource {
    override val firestoreOnline: Flow<Boolean?> = flowOf(null)
}
