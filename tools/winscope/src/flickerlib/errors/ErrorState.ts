/*
 * Copyright 2020, The Android Open Source Project
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

import { ErrorState } from "../common";
import Error from './Error';

ErrorState.fromProto = function (protos: any[], timestamp: number): ErrorState {
    const errors = protos.map(it => Error.fromProto(it));
    const state = new ErrorState(errors, `${timestamp}`);
    return state;
}

export default ErrorState;
