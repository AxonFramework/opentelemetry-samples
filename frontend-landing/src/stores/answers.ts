/*
 * Copyright (c) 2023-2023. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import {defineStore} from "pinia";
import {questions} from "@/stores/quiz";

export const answerStore = defineStore('answers', () => {
    const currentAnswers = JSON.parse(localStorage.getItem('__answers') || '') as { [questionId: number]: string }

    const setAnswer = (question: number, answer: string) => {
        currentAnswers[question] = answer
        localStorage.setItem('__answers', JSON.stringify(currentAnswers))
    }

    const currentQuestion = () => {
        questions.sort((a, b) => b.index - a.index).find(a => !currentAnswers[a.index])
    }

    return {
        setAnswer,
        currentAnswers,
        currentQuestion,
    }
})
