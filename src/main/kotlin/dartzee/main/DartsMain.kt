package dartzee.main

import dartzee.`object`.DartsClient
import dartzee.core.util.DialogUtil
import dartzee.core.util.MessageDialogFactory
import dartzee.logging.LoggerUncaughtExceptionHandler
import dartzee.screen.ScreenCache
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    DartsClient.parseProgramArguments(args)

    if (!DartsClient.trueLaunch && DartsClient.isWindowsOs())
    {
        Runtime.getRuntime().exec("cmd /c start javaw -Xms256m -Xmx512m -jar Dartzee.jar trueLaunch")
        exitProcess(0)
    }

    DialogUtil.init(MessageDialogFactory())

    setLoggingContextFields()

    setLookAndFeel()

    val mainScreen = ScreenCache.mainScreen
    Thread.setDefaultUncaughtExceptionHandler(LoggerUncaughtExceptionHandler())

    DartsClient.logArgumentState()

    DartsClient.checkForUpdatesIfRequired()

    mainScreen.isVisible = true
    mainScreen.init()
}
