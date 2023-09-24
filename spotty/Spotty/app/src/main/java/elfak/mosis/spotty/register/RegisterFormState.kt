package elfak.mosis.spotty.register

data class RegisterFormState (
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val confpassError: Int? = null,
    val phoneError: Int? = null,
    val firstnameError: Int? = null,
    val lastnameError: Int? = null,
    val pictureError: Int? = null,
    val isDataValid: Boolean = false
)