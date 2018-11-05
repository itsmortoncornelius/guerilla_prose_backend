import model.User

fun User.isEmpty(): Boolean {
    return this.email.isNullOrEmpty()
            && this.firstname.isNullOrEmpty()
            && this.lastname.isNullOrEmpty()
}