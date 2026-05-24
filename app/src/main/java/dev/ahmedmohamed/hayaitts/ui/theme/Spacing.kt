package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.ui.unit.dp

// Single source of truth for all in-app spacing. Every screen's root scroll
// container is responsible for `screenHorizontal`; cards and section blocks
// stop adding horizontal padding of their own so paddings can't stack.
//
// Numbers are deliberately uneven (16 / 12 / 8 instead of 16 / 16 / 16) so
// nested rhythms differ at small enough increments to feel grouped without
// reading as "the same gap twice."
object Spacing {
    val screenHorizontal = 16.dp
    val sectionVertical = 16.dp
    val itemSpacing = 12.dp
    val chipSpacing = 8.dp
    val cardInsetVertical = 16.dp
}
