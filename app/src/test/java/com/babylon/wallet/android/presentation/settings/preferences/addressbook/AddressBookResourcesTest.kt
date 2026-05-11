package com.babylon.wallet.android.presentation.settings.preferences.addressbook

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AddressBookResourcesTest {

    @Test
    fun `address book copy matches iOS`() {
        val expected = mapOf(
            "addressBook_emptyState" to "No saved addresses yet",
            "addressBook_deleteAlertTitle" to "Delete Saved Address?",
            "addressBook_deleteAlertMessage" to "Are you sure you want to remove this saved address?",
            "addressBook_entryForm_ownAccountAlertTitle" to "Cannot Save This Address",
            "addressBook_entryForm_ownAccountAlertMessage" to "This address belongs to one of your accounts and cannot be added to Address Book."
        )

        listOf("values", "values-en").forEach { resDir ->
            val strings = readStrings(resDir)

            expected.forEach { (key, value) ->
                assertEquals("$key in $resDir", value, strings[key])
            }
        }
    }

    private fun readStrings(resDir: String): Map<String, String> {
        val file = listOf(
            File("src/main/res/$resDir/strings.xml"),
            File("app/src/main/res/$resDir/strings.xml")
        ).first(File::exists)

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)

        return (0 until document.getElementsByTagName("string").length).associate { index ->
            val node = document.getElementsByTagName("string").item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
