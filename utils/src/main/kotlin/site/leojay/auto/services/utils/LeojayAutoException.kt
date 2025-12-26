package site.leojay.auto.services.utils


/**
 * 自动工具统一异常
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class LeojayAutoException : Exception {
    constructor() : super()
    constructor(cause: ClassNotFoundException) : super(cause)
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )

}