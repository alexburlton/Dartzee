package dartzee.core.util

import com.github.alexburlton.swingtest.flushEdt
import dartzee.core.helper.TestMessageDialogFactory
import dartzee.helper.AbstractTest
import dartzee.helper.TEST_ROOT
import dartzee.logging.CODE_DIALOG_CLOSED
import dartzee.logging.CODE_DIALOG_SHOWN
import dartzee.logging.Severity
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import java.io.File
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class TestDialogUtil: AbstractTest()
{
    var factoryMock = mockk<TestMessageDialogFactory>(relaxed = true)

    @Test
    fun `Should pass method calls on to implementation`()
    {
        DialogUtil.init(factoryMock)

        DialogUtil.showInfo("Info")
        DialogUtil.showLoadingDialog("Loading...")
        DialogUtil.showQuestion("Q")
        DialogUtil.dismissLoadingDialog()
        DialogUtil.showOption("Free Pizza", "Would you like some?", listOf("Yes please", "No thanks"))
        DialogUtil.chooseDirectory(null)

        verifySequence {
            factoryMock.showInfo("Info")
            factoryMock.showLoading("Loading...")
            factoryMock.showQuestion("Q", false)
            factoryMock.dismissLoading()
            factoryMock.showOption("Free Pizza", "Would you like some?", listOf("Yes please", "No thanks"))
            factoryMock.chooseDirectory(null)
        }

        clearAllMocks()

        DialogUtil.showError("Test")

        verifySequence {
            factoryMock.dismissLoading()
            factoryMock.showError("Test")
        }

        clearAllMocks()
    }

    @Test
    fun `Should log for INFO dialogs`()
    {
        DialogUtil.showInfo("Something useful")

        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Info dialog shown: Something useful"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Info dialog closed"
    }

    @Test
    fun `Should log for ERROR dialogs`()
    {
        DialogUtil.showError("Something bad")

        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Error dialog shown: Something bad"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Error dialog closed"
    }

    @Test
    fun `Should show an ERROR dialog later`()
    {
        SwingUtilities.invokeLater { Thread.sleep(500) }
        DialogUtil.showErrorLater("Some error")
        dialogFactory.errorsShown.shouldBeEmpty()

        flushEdt()
        dialogFactory.errorsShown.shouldContainExactly("Some error")
    }

    @Test
    fun `Should log for QUESTION dialogs, with the correct selection`()
    {
        dialogFactory.questionOption = JOptionPane.YES_OPTION

        DialogUtil.showQuestion("Do you like cheese?")
        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Question dialog shown: Do you like cheese?"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Question dialog closed - selected Yes"

        clearLogs()

        dialogFactory.questionOption = JOptionPane.NO_OPTION
        DialogUtil.showQuestion("Do you like mushrooms?")
        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Question dialog shown: Do you like mushrooms?"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Question dialog closed - selected No"

        clearLogs()

        dialogFactory.questionOption = JOptionPane.CANCEL_OPTION
        DialogUtil.showQuestion("Do you want to delete all data?", true)
        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Question dialog shown: Do you want to delete all data?"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Question dialog closed - selected Cancel"
    }

    @Test
    fun `Should log when showing and dismissing loading dialog`()
    {
        DialogUtil.showLoadingDialog("One moment...")
        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Loading dialog shown: One moment..."

        DialogUtil.dismissLoadingDialog()
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Loading dialog closed"
    }

    @Test
    fun `Should not log if loading dialog wasn't visible`()
    {
        DialogUtil.dismissLoadingDialog()
        verifyNoLogs(CODE_DIALOG_CLOSED)
    }

    @Test
    fun `Should log for OPTION dialogs, with the selection`()
    {
        dialogFactory.optionSequence.add("Yes please")

        DialogUtil.showOption("Free Pizza", "Free pizza?", listOf("Yes please", "No thanks"))
        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Option dialog shown: Free pizza?"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Option dialog closed - selected Yes please"
    }

    @Test
    fun `Should log for INPUT dialogs, with the selection`()
    {
        dialogFactory.inputSelection = "Camembert"
        DialogUtil.showInput<String>("Cheezoid", "Enter your favourite cheese")

        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "Input dialog shown: Enter your favourite cheese"
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "Input dialog closed - selected Camembert"
    }

    @Test
    fun `Should handle a null file when logging file selection`()
    {
        dialogFactory.directoryToSelect = null

        DialogUtil.chooseDirectory(null)

        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "File selector dialog shown: "
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "File selector dialog closed"
    }

    @Test
    fun `Should log the file path when selecting a directory`()
    {
        val f = File(TEST_ROOT)
        dialogFactory.directoryToSelect = f

        DialogUtil.chooseDirectory(null)

        verifyLog(CODE_DIALOG_SHOWN, Severity.INFO).message shouldBe "File selector dialog shown: "
        verifyLog(CODE_DIALOG_CLOSED, Severity.INFO).message shouldBe "File selector dialog closed - selected ${f.absolutePath}"
    }

}