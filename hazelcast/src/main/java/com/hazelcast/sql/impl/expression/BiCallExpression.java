/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.expression;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;

/**
 * Expression with two operands.
 */
public abstract class BiCallExpression<T> implements CallExpression<T> {
    /** First operand. */
    protected Expression operand1;

    /** Second operand. */
    protected Expression operand2;

    protected BiCallExpression() {
        // No-op.
    }

    protected BiCallExpression(Expression operand1, Expression operand2) {
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeObject(operand1);
        out.writeObject(operand2);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        operand1 = in.readObject();
        operand2 = in.readObject();
    }
}
