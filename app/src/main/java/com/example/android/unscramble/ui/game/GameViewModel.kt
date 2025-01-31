/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.unscramble.ui.game

import android.text.Spannable
import android.text.SpannableString
import android.text.style.TtsSpan
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.random.Random

/**
 * ViewModel containing the app data and methods to process the data
 */

class SaveableMutableStateFlow<T>(
    private val saveStateHandler: SavedStateHandle,
    private val key: String,
    initialValue: T
) {
    private val state: StateFlow<T> = saveStateHandler.getStateFlow(key, initialValue)
    var value: T
        get() = state.value
        set(value) {
            saveStateHandler[key] = value
        }

    fun asStateFlow(): StateFlow<T> = state
}

fun <T> SavedStateHandle.getMutableStateFlow(key: String, value: T): SaveableMutableStateFlow<T> =
    SaveableMutableStateFlow(this, key, value)

// stateHandler는 Fragment에서 자동으로 주입해준다
class GameViewModel(private val stateHandler: SavedStateHandle) : ViewModel() {
//    val score: StateFlow<Int> = stateHandler.getStateFlow("score", 0)
//    private fun setScore(value: Int) {
//        stateHandler["score"] = value
//    }

    private val _score = stateHandler.getMutableStateFlow("score", 0)
    val score: StateFlow<Int>
        get() = _score.asStateFlow()


    private val _currentWordCount = stateHandler.getMutableStateFlow("wordCount", 0)
    val currentWordCount: StateFlow<Int>
        get() = _currentWordCount.asStateFlow()

    private val _currentScrambledWord = stateHandler.getMutableStateFlow("scrambleWord", "")
    val currentScrambledWord: StateFlow<Spannable> = _currentScrambledWord
        .asStateFlow()
        .onSubscription {
            if (currentWord.isEmpty())
                nextWord()
        }
        .map {scrambledWord ->
            val spannable: Spannable = SpannableString(scrambledWord)
            spannable.setSpan(
                TtsSpan.VerbatimBuilder(scrambledWord).build(),
                0,
                scrambledWord.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            spannable
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpannableString(""))

    // List of words used in the game
    private var wordsList: List<String>
        get() = stateHandler["wordsList"] ?: emptyList()
        set(value) {
            stateHandler["wordsList"] = value
        }
    private var currentWord: String
        get() = stateHandler["currentWord"] ?: ""
        set(value) {
            val tempWord = currentWord.toCharArray()
            tempWord.shuffle()

            do {
                tempWord.shuffle()
            } while (String(tempWord) == value)
            stateHandler["currentWord"] = value

            Log.d("Unscramble", "currentWord= $currentWord")
            _currentScrambledWord.value = String(tempWord)
            _currentWordCount.value += 1
            wordsList = wordsList + currentWord
        }

    private var isGameOver: Boolean = false

    /*
     * Updates currentWord and currentScrambledWord with the next word.
     */
//    private fun getNextWord() {
//        var nextWord: String
//        do {
//            nextWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
//        } while (wordsList.contains(nextWord))
//        currentWord = nextWord
//    }

    /*
     * Re-initializes the game data to restart the game.
     */
    fun reinitializeData() {
        _score.value = 0
        _currentWordCount.value = 0
        wordsList = emptyList()
        nextWord()
        isGameOver = false
    }

    /*
    * Increases the game score if the player’s word is correct.
    */
    private fun increaseScore() {
        _score.value += SCORE_INCREASE
    }

    /*
    * Returns true if the player word is correct.
    * Increases the score accordingly.
    */
    fun isUserWordCorrect(playerWord: String): Boolean {
        if (playerWord.equals(currentWord, true)) {
            increaseScore()
            return true
        }
        return false
    }

    /*
    * Returns true if the current word count is less than MAX_NO_OF_WORDS
    */
    fun nextWord(): Boolean {
        return if (_currentWordCount.value < MAX_NO_OF_WORDS) {
            var nextWord: String
            do {
                nextWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
            } while (wordsList.contains(nextWord))
            currentWord = nextWord
            true
        } else {
            isGameOver = true
            false
        }
    }

    fun isGameOver() = isGameOver
}
