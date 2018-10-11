package model

import java.time.LocalDate

data class GuerillaProse(
        var id: Int,
        val text: String?,
        val imageUrl: String?,
        val label: String,
        val userId: Int,
        val date: LocalDate
)