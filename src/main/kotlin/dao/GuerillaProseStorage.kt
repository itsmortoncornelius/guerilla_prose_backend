package dao

import model.GuerillaProse
import model.User
import java.io.Closeable

interface GuerillaProseStorage: Closeable {
    fun createGuerillaProse(guerillaProse: GuerillaProse): GuerillaProse
    fun getGuerillaProses(): List<GuerillaProse>
    fun getGuerillaProsesForLabel(label: String): List<GuerillaProse>
    fun getGuerillaProsesForUser(userId: Int): List<GuerillaProse>
    fun getGuerillaProse(id: Int): GuerillaProse?

    fun createUser(user: User): User
    fun getUser(userId: Int): User?
}