package de.maxr1998.modernpreferences.preferences.choice

fun interface OnItemClickListener {
    /**
     * Notified when the user clicks a [SelectionItem].
     * This is called before the change gets persisted and can be prevented by returning false.
     *
     * @param item the clicked item
     *
     * @return true to to allow the selection of the item
     */
    fun onItemSelected(item: SelectionItem): Boolean
}