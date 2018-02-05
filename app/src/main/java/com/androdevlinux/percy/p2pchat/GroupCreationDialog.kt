package com.androdevlinux.percy.p2pchat

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText


class GroupCreationDialog : DialogFragment() {

    private var listener: GroupCreationAcceptButtonListener? = null


    interface GroupCreationAcceptButtonListener {
        fun onAcceptButtonListener(groupName: String)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)
        val layoutInflater = activity!!.layoutInflater
        val dialogContent = layoutInflater.inflate(R.layout.creation_group_dialog, null)

        val tfGroupName = dialogContent.findViewById<View>(R.id.editTextGroupName) as EditText

        builder.setView(dialogContent)
        builder.setPositiveButton(resources.getString(R.string.btn_accept)) { dialog, which ->
            val groupName = tfGroupName.text.toString()
            if (listener != null) {
                listener!!.onAcceptButtonListener(groupName)
            }
        }

        builder.setNegativeButton(resources.getString(R.string.btn_cancel)) { dialog, which -> this@GroupCreationDialog.dialog.cancel() }

        return builder.create()
    }

    fun addGroupCreationAcceptListener(listener: GroupCreationAcceptButtonListener) {
        this.listener = listener
    }

}
