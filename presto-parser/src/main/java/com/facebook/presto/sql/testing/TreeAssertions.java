/*
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
package com.facebook.presto.sql.testing;

import com.facebook.presto.sql.parser.ParsingException;
import com.facebook.presto.sql.tree.DefaultTraversalVisitor;
import com.facebook.presto.sql.tree.Node;
import com.facebook.presto.sql.tree.Statement;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

import java.util.List;

import static com.facebook.presto.sql.SqlFormatter.formatSql;
import static com.facebook.presto.sql.parser.SqlParser.createStatement;
import static java.lang.String.format;

public final class TreeAssertions
{
    private TreeAssertions() {}

    public static void assertFormattedSql(Node expected)
    {
        String formatted = formatSql(expected);

        // verify round-trip of formatting already-formatted SQL
        Statement actual = parseFormatted(formatted, expected);
        assertEquals(formatSql(actual), formatted);

        // compare parsed tree with parsed tree of formatted SQL
        if (!actual.equals(expected)) {
            // simplify finding the non-equal part of the tree
            assertListEquals(linearizeTree(actual), linearizeTree(expected));
        }
        assertEquals(actual, expected);
    }

    private static Statement parseFormatted(String sql, Node tree)
    {
        try {
            return createStatement(sql);
        }
        catch (ParsingException e) {
            throw new AssertionError(format(
                    "failed to parse formatted SQL: %s\nerror: %s\ntree: %s",
                    sql, e.getMessage(), tree));
        }
    }

    private static List<Node> linearizeTree(Node tree)
    {
        final ImmutableList.Builder<Node> nodes = ImmutableList.builder();
        new DefaultTraversalVisitor<Node, Void>()
        {
            @Override
            public Node process(Node node, @Nullable Void context)
            {
                Node result = super.process(node, context);
                nodes.add(node);
                return result;
            }
        }.process(tree, null);
        return nodes.build();
    }

    private static <T> void assertListEquals(List<T> actual, List<T> expected)
    {
        if (actual.size() != expected.size()) {
            Joiner joiner = Joiner.on("\n    ");
            throw new AssertionError(format(
                    "Lists not equal%nActual [%s]:%n    %s%nExpected [%s]:%n    %s%n",
                    actual.size(), joiner.join(actual),
                    expected.size(), joiner.join(expected)));
        }
        assertEquals(actual, expected);
    }

    private static <T> void assertEquals(T actual, T expected)
    {
        if (!actual.equals(expected)) {
            throw new AssertionError(format("expected [%s] but found [%s]", expected, actual));
        }
    }
}
