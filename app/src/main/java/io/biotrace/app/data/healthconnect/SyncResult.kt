package io.biotrace.app.data.healthconnect

data class SyncResult(
    val recordsSynced: Int,
    val errors: List<String> = emptyList()
) {
    operator fun plus(other: SyncResult): SyncResult {
        return SyncResult(
            recordsSynced = this.recordsSynced + other.recordsSynced,
            errors = this.errors + other.errors
        )
    }
}
