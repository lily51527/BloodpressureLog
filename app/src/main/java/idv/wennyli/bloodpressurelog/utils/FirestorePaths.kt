package idv.wennyli.bloodpressurelog.utils

object FirestorePaths {
    private const val COLLECTION_ARTIFACTS = "artifacts"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_BLOOD_PRESSURES = "bloodPressures"

    fun bloodPressures(appId: String, userId: String): String =
        "$COLLECTION_ARTIFACTS/$appId/$COLLECTION_USERS/$userId/$COLLECTION_BLOOD_PRESSURES"
}
