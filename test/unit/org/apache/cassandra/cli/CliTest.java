/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.cli;

import org.apache.cassandra.CleanupHelper;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.thrift.transport.TTransportException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CliTest extends CleanupHelper
{
    // please add new statements here so they could be auto-runned by this test.
    private String[] statements = {
        "use TestKeySpace;",
        "create column family CF1 with comparator=UTF8Type and column_metadata=[{ column_name:world, validation_class:IntegerType, index_type:0, index_name:IdxName }, { column_name:world2, validation_class:LongType, index_type:KEYS, index_name:LongIdxName}];",
        "assume CF1 keys as utf8;",
        "set CF1[hello][world] = 123848374878933948398384;",
        "set CF1[hello][test_quote] = 'value\\'';",
        "set CF1['k\\'ey'][VALUE] = 'VAL';",
        "set CF1['k\\'ey'][VALUE] = 'VAL\\'';",
        "set CF1[hello][-31337] = 'some string value';",
        "get CF1[hello][-31337];",
        "get CF1[hello][world];",
        "get CF1[hello][test_quote];",
        "get CF1['k\\'ey'][VALUE]",
        "set CF1[hello][-31337] = -23876;",
        "set CF1[hello][-31337] = long(-23876);",
        "set CF1[hello][world2] = 15;",
        "get CF1 where world2 = long(15);",
        "get cF1 where world2 = long(15);",
        "get Cf1 where world2 = long(15);",
        "set CF1['hello'][time_spent_uuid] = timeuuid(a8098c1a-f86e-11da-bd1a-00112444be1e);",
        "create column family CF2 with comparator=IntegerType;",
        "assume CF2 keys as utf8;",
        "set CF2['key'][98349387493847748398334] = 'some text';",
        "get CF2['key'][98349387493847748398334];",
        "set CF2['key'][98349387493] = 'some text other';",
        "get CF2['key'][98349387493];",
        "create column family CF3 with comparator=UTF8Type and column_metadata=[{column_name:'big world', validation_class:LongType}];",
        "assume CF3 keys as utf8;",
        "set CF3['hello']['big world'] = 3748;",
        "get CF3['hello']['big world'];",
        "list CF3;",
        "list CF3[:];",
        "list CF3[h:];",
        "list CF3 limit 10;",
        "list CF3[h:] limit 10;",
        "create column family CF4 with comparator=IntegerType and column_metadata=[{column_name:9999, validation_class:LongType}];",
        "assume CF4 keys as utf8;",
        "set CF4['hello'][9999] = 1234;",
        "get CF4['hello'][9999];",
        "get CF4['hello'][9999] as Long;",
        "get CF4['hello'][9999] as Bytes;",
        "set CF4['hello'][9999] = Long(1234);",
        "get CF4['hello'][9999];",
        "get CF4['hello'][9999] as Long;",
        "del CF4['hello'][9999];",
        "get CF4['hello'][9999];",
        "create column family SCF1 with column_type=Super and comparator=IntegerType and subcomparator=LongType and column_metadata=[{column_name:9999, validation_class:LongType}];",
        "assume SCF1 keys as utf8;",
        "set SCF1['hello'][1][9999] = 1234;",
        "get SCF1['hello'][1][9999];",
        "get SCF1['hello'][1][9999] as Long;",
        "get SCF1['hello'][1][9999] as Bytes;",
        "set SCF1['hello'][1][9999] = Long(1234);",
        "set SCF1['hello'][-1][-12] = Long(5678);",
        "get SCF1['hello'][-1][-12];",
        "set SCF1['hello'][-1][-12] = -340897;",
        "set SCF1['hello'][-1][-12] = integer(-340897);",
        "get SCF1['hello'][1][9999];",
        "get SCF1['hello'][1][9999] as Long;",
        "del SCF1['hello'][1][9999];",
        "get SCF1['hello'][1][9999];",
        "set SCF1['hello'][1][9999] = Long(1234);",
        "del SCF1['hello'][9999];",
        "get SCF1['hello'][1][9999];",
        "create column family Counter1 with comparator=UTF8Type and default_validation_class=CounterColumnType;",
        "assume Counter1 keys as utf8;",
        "incr Counter1['hello']['cassandra'];",
        "incr Counter1['hello']['cassandra'] by 3;",
        "incr Counter1['hello']['cassandra'] by -2;",
        "decr Counter1['hello']['cassandra'];",
        "decr Counter1['hello']['cassandra'] by 3;",
        "decr Counter1['hello']['cassandra'] by -2;",
        "get Counter1['hello']['cassandra'];",
        "get Counter1['hello'];",
        "truncate CF1;",
        "drop index on CF1.world2;",
        "update keyspace TestKeySpace with placement_strategy='org.apache.cassandra.locator.LocalStrategy' and durable_writes = false;",
        "update keyspace TestKeySpace with strategy_options=[{DC1:3, DC2:4, DC5:1}];",
        "assume CF1 comparator as utf8;",
        "assume CF1 sub_comparator as integer;",
        "assume CF1 validator as lexicaluuid;",
        "assume CF1 keys as timeuuid;",
        "create column family CF7;",
        "assume CF7 keys as utf8;",
        "set CF7[1][timeuuid()] = utf8(test1);",
        "set CF7[2][lexicaluuid()] = utf8('hello world!');",
        "set CF7[3][lexicaluuid(550e8400-e29b-41d4-a716-446655440000)] = utf8(test2);",
        "set CF7[key2][timeuuid()] = utf8(test3);",
        "assume CF7 comparator as lexicaluuid;",
        "assume CF7 keys as utf8;",
        "list CF7;",
        "get CF7[3];",
        "get CF7[3][lexicaluuid(550e8400-e29b-41d4-a716-446655440000)];",
        "get sCf1['hello'][1][9999];",
        "set sCf1['hello'][1][9999] = 938;",
        "set sCf1['hello'][1][9999] = 938 with ttl = 30;",
        "set sCf1['hello'][1][9999] = 938 with ttl = 560;",
        "list sCf1;",
        "del SCF1['hello'][1][9999];",
        "assume sCf1 comparator as utf8;",
        "create column family CF8;",
        "drop column family cF8;",
        "create keyspace TESTIN;",
        "drop keyspace tesTIN;",
        "create column family myCF with column_type='Super' and comparator='UTF8Type' AND subcomparator='UTF8Type';",
        "assume myCF keys as utf8;",
        "create column family Countries with comparator=UTF8Type and column_metadata=[ {column_name: name, validation_class: UTF8Type} ];",
        "set Countries[1][name] = USA;",
        "get Countries[1][name];",
        "set myCF['key']['scName']['firstname'] = 'John';",
        "get myCF['key']['scName']",
        "assume CF3 keys as utf8;",
        "use TestKEYSpace;",
        "describe cluster;",
        "help describe cluster;",
        "show cluster name",
        "show api version",
        "help help",
        "help connect",
        "help use",
        "help describe KEYSPACE",
        "HELP exit",
        "help QUIT",
        "help show cluster name",
        "help show keyspaces",
        "help show api version",
        "help create keyspace",
        "HELP update KEYSPACE",
        "HELP CREATE column FAMILY",
        "HELP UPDATE COLUMN family",
        "HELP drop keyspace",
        "help drop column family",
        "HELP GET",
        "HELP set",
        "HELP DEL",
        "HELP count",
        "HELP list",
        "HELP TRUNCATE",
        "help assume",
        "HELP",
        "?"
    };
   
    @Test
    public void testCli() throws IOException, TTransportException, ConfigurationException
    {
        new EmbeddedCassandraService().start();

        // new error/output streams for CliSessionState
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        // checking if we can connect to the running cassandra node on localhost
        CliMain.connect("127.0.0.1", 9170);

        // setting new output stream
        CliMain.sessionState.setOut(new PrintStream(outStream));
        CliMain.sessionState.setErr(new PrintStream(errStream));

        // re-creating keyspace for tests
        // dropping in case it exists e.g. could be left from previous run
        CliMain.processStatement("drop keyspace TestKeySpace;");
        CliMain.processStatement("create keyspace TestKeySpace;");

        for (String statement : statements)
        {
            errStream.reset();
            // System.out.println("Executing statement: " + statement);
            CliMain.processStatement(statement);
            String result = outStream.toString();
            // System.out.println("Result:\n" + result);
            assertEquals(errStream.toString() + " processing " + statement, "", errStream.toString());
            if (statement.startsWith("drop ") || statement.startsWith("create ") || statement.startsWith("update "))
            {
                assert Pattern.compile("(.{8})-(.{4})-(.{4})-(.{4})-(.{12}).*", Pattern.DOTALL).matcher(result).matches() : result;
            }
            else if (statement.startsWith("set "))
            {
                assertEquals(result, "Value inserted." + System.getProperty("line.separator"));
            }
            else if (statement.startsWith("incr "))
            {
                assertEquals(result, "Value incremented." + System.getProperty("line.separator"));
            }
            else if (statement.startsWith("decr "))
            {
                assertEquals(result, "Value decremented." + System.getProperty("line.separator"));
            }
            else if (statement.startsWith("get "))
            {
                if (statement.contains("where"))
                {
                    assertTrue(result.startsWith("-------------------" + System.getProperty("line.separator") + "RowKey:"));
                }
                else if (statement.contains("Counter"))
                {
                    assertTrue(result.startsWith("=> (counter=") || result.startsWith("Value was not found"));
                }
                else
                {
                    assertTrue(result.startsWith("=> (column=") || result.startsWith("Value was not found"));
                }
            }
            else if (statement.startsWith("truncate "))
            {
                assertTrue(result.contains(" truncated."));
            }
            else if (statement.startsWith("assume "))
            {
                assertTrue(result.contains("successfully."));
            }

            outStream.reset(); // reset stream so we have only output from next statement all the time
            errStream.reset(); // no errors to the end user.
        }
    }

    @Test
    public void testEscape()
    {
        //escaped is the string read from the cli.
        String escaped = "backspace \\b tab \\t linefeed \\n form feed \\f carriage return \\r duble quote \\\" " +
                "single quote \\' backslash \\\\";
        String unescaped = "backspace \b tab \t linefeed \n form feed \f carriage return \r duble quote \" " +
                "single quote ' backslash \\";
        // when read from the cli may have single quotes around it
        assertEquals(unescaped, CliUtils.unescapeSQLString("'" + escaped + "'"));
        assertEquals(escaped, CliUtils.escapeSQLString(unescaped));
    }
}