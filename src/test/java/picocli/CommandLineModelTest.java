/*
   Copyright 2017 Remko Popma

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package picocli;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import picocli.CommandLine.*;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.*;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.*;

import static org.junit.Assert.*;
import static picocli.HelpTestUtil.*;
import static picocli.HelpTestUtil.versionString;


public class CommandLineModelTest {
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog().muteForSuccessfulTests();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Test
    public void testEmptyModelUsageHelp() throws Exception {
        CommandSpec spec = CommandSpec.create();
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        assertEquals(String.format("Usage: <main class>%n"), actual);
    }

    @Test
    public void testEmptyModelParse() throws Exception {
        setTraceLevel("OFF");
        CommandSpec spec = CommandSpec.create();
        CommandLine commandLine = new CommandLine(spec);
        commandLine.setUnmatchedArgumentsAllowed(true);
        commandLine.parse("-p", "123", "abc");
        assertEquals(Arrays.asList("-p", "123", "abc"), commandLine.getUnmatchedArguments());
    }
    
    @Test
    public void testModelUsageHelp() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addOption(OptionSpec.builder("-h", "--help").usageHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-V", "--version").versionHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-c", "--count").paramLabel("COUNT").arity("1").type(int.class).description("number of times to execute").build());
        spec.addOption(OptionSpec.builder("-f", "--fix").paramLabel("FIXED(BOOLEAN)").arity("1").hideParamSyntax(true).required(true).description("run with fixed option").build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> [-hV] [-c=COUNT] -f=FIXED(BOOLEAN)%n" +
                "  -c, --count=COUNT          number of times to execute%n" +
                "  -f, --fix=FIXED(BOOLEAN)   run with fixed option%n" +
                "  -h, --help                 show help and exit%n" +
                "  -V, --version              show help and exit%n");
        assertEquals(expected, actual);
    }
    
    @Test
    public void testModelUsageHelpWithCustomSeparator() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addOption(OptionSpec.builder("-h", "--help").usageHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-V", "--version").versionHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-c", "--count").paramLabel("COUNT").arity("1").type(int.class).description("number of times to execute").build());
        spec.addOption(OptionSpec.builder("-f", "--fix").paramLabel("FIXED(=BOOLEAN)").arity("1").hideParamSyntax(true).required(true).description("run with fixed option").build());
        CommandLine commandLine = new CommandLine(spec).setSeparator(" ");
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> [-hV] [-c COUNT] -f FIXED(=BOOLEAN)%n" +
                "  -c, --count COUNT   number of times to execute%n" +
                "  -f, --fix FIXED(=BOOLEAN)%n" +
                "                      run with fixed option%n" +
                "  -h, --help          show help and exit%n" +
                "  -V, --version       show help and exit%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelpPositional_empty() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addPositional(PositionalParamSpec.builder().build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> PARAM...%n" +
                "      PARAM...%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelpPositional_withDescription() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addPositional(PositionalParamSpec.builder().description("positional param").build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> PARAM...%n" +
                "      PARAM...   positional param%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelp_emptyWithAutoHelpMixin() throws Exception {
        CommandSpec spec = CommandSpec.create().addMixin("auto", CommandSpec.forAnnotatedObject(new AutoHelpMixin()));
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> [-hV]%n" +
                "  -h, --help      Show this help message and exit.%n" +
                "  -V, --version   Print version information and exit.%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelp_CustomizedUsageMessage() throws Exception {
        CommandSpec spec = CommandSpec.create().addMixin("auto", CommandSpec.forAnnotatedObject(new AutoHelpMixin()));
        spec.name("the awesome util");
        spec.usageMessage()
                .descriptionHeading("Description heading%n")
                .description("description line 1", "description line 2")
                .footerHeading("Footer heading%n")
                .footer("footer line 1", "footer line 2")
                .headerHeading("Header heading%n")
                .header("header line 1", "header line 2")
                .optionListHeading("Options%n")
                .parameterListHeading("Positional Parameters%n");
        spec.addPositional(PositionalParamSpec.builder().description("positional param").build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Header heading%n" +
                "header line 1%n" +
                "header line 2%n" +
                "Usage: the awesome util [-hV] PARAM...%n" +
                "Description heading%n" +
                "description line 1%n" +
                "description line 2%n" +
                "Positional Parameters%n" +
                "      PARAM...    positional param%n" +
                "Options%n" +
                "  -h, --help      Show this help message and exit.%n" +
                "  -V, --version   Print version information and exit.%n" +
                "Footer heading%n" +
                "footer line 1%n" +
                "footer line 2%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelp_abbreviateSynopsisWithoutPositional() throws UnsupportedEncodingException {
        CommandSpec spec = CommandSpec.create();
        spec.usageMessage().abbreviateSynopsis(true).requiredOptionMarker('!').sortOptions(false);
        spec.addOption(OptionSpec.builder("-x").required(true).description("required").build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> [OPTIONS]%n" +
                "! -x     required%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelp_abbreviateSynopsisWithPositional() throws UnsupportedEncodingException {
        CommandSpec spec = CommandSpec.create();
        spec.usageMessage().abbreviateSynopsis(true).requiredOptionMarker('!').sortOptions(false);
        spec.addOption(OptionSpec.builder("-x").required(true).description("required").build());
        spec.addPositional(PositionalParamSpec.builder().arity("1").paramLabel("POSITIONAL").description("positional").build());
        CommandLine commandLine = new CommandLine(spec);
        String actual = usageString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "Usage: <main class> [OPTIONS] POSITIONAL...%n" +
                "!     POSITIONAL...   positional%n" +
                "! -x                  required%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testUsageHelp_width_default80() {
        assertEquals(80, UsageMessageSpec.DEFAULT_USAGE_WIDTH);
        assertEquals(UsageMessageSpec.DEFAULT_USAGE_WIDTH, new UsageMessageSpec().width());
    }

    @Test
    public void testUsageHelp_width_configurableWithSystemProperty() {
        System.setProperty("picocli.usage.width", "67");
        try {
            assertEquals(67, new UsageMessageSpec().width());
        } finally {
            System.clearProperty("picocli.usage.width");
        }
    }

    @Test
    public void testUsageHelp_width_SystemPropertyOverrulesSetValue() {
        System.setProperty("picocli.usage.width", "67");
        try {
            assertEquals(67, new UsageMessageSpec().width(123).width());
        } finally {
            System.clearProperty("picocli.usage.width");
        }
    }

    @Test
    public void testUsageHelp_width_setter() {
        UsageMessageSpec spec = new UsageMessageSpec();
        spec.width(67);
        assertEquals(67, spec.width());
    }

    @Test(expected = InitializationException.class)
    public void testUsageHelp_width_setterDisallowsValuesBelow55() {
        new UsageMessageSpec().width(54);
    }

    @Test
    public void testUsageHelp_width_setterAllowsValuesAt55OrHigher() {
        assertEquals(55, new UsageMessageSpec().width(55).width());
        assertEquals(Integer.MAX_VALUE, new UsageMessageSpec().width(Integer.MAX_VALUE).width());
    }

    @Test
    public void testVersionHelp_basic() throws Exception {
        CommandSpec spec = CommandSpec.create().version("1.0", "copyright etc");
        CommandLine commandLine = new CommandLine(spec);
        String actual = versionString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "1.0%n" +
                "copyright etc%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testVersionHelp_versionProvider() throws Exception {
        IVersionProvider provider = new IVersionProvider() {
            public String[] getVersion() throws Exception {
                return new String[] {"2.0", "by provider"};
            }
        };
        CommandSpec spec = CommandSpec.create().versionProvider(provider);
        CommandLine commandLine = new CommandLine(spec);
        String actual = versionString(commandLine, Ansi.OFF);
        String expected = String.format("" +
                "2.0%n" +
                "by provider%n");
        assertEquals(expected, actual);
    }

    @Test
    public void testVersionHelp_helpCommand() {
        CommandSpec helpCommand = CommandSpec.create().helpCommand(true);
        assertTrue(helpCommand.helpCommand());

        CommandSpec parent = CommandSpec.create().addOption(OptionSpec.builder("-x").required(true).build());
        parent.addSubcommand("help", helpCommand);

        CommandLine commandLine = new CommandLine(parent);
        commandLine.parse("help"); // no missing param exception

        try {
            commandLine.parse();
        } catch (MissingParameterException ex) {
            assertEquals("Missing required option '-x=PARAM'", ex.getMessage());
            assertEquals(1, ex.getMissing().size());
            assertSame(ex.getMissing().get(0).toString(), parent.posixOptionsMap().get('x'), ex.getMissing().get(0));
        }
    }

    @Test
    public void testModelParse() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addOption(OptionSpec.builder("-h", "--help").usageHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-V", "--version").versionHelp(true).description("show help and exit").build());
        spec.addOption(OptionSpec.builder("-c", "--count").paramLabel("COUNT").arity("1").type(int.class).description("number of times to execute").build());
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "33");
        assertEquals(Integer.valueOf(33), spec.optionsMap().get("-c").getValue());
    } // TODO parse method should return an object offering only the options/positionals that were matched

    @Test
    public void testMultiValueOptionArityAloneIsInsufficient() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(int.class).build();
        assertFalse(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        try {
            commandLine.parse("-c", "1", "2", "3");
            fail("Expected exception");
        } catch (UnmatchedArgumentException ex) {
            assertEquals("Unmatched arguments: 2, 3", ex.getMessage());
        }
    }

    @Test
    public void testMultiValuePositionalParamArityAloneIsInsufficient() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(int.class).build();
        assertFalse(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        try {
            commandLine.parse("1", "2", "3");
            fail("Expected exception");
        } catch (UnmatchedArgumentException ex) {
            assertEquals("Unmatched arguments: 2, 3", ex.getMessage());
        }
    }

    @Test
    public void testMultiValueOptionWithArray() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(int[].class).build();
        assertTrue(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "1", "2", "3");
        assertArrayEquals(new int[] {1, 2, 3}, (int[]) spec.optionsMap().get("-c").getValue());
    }

    @Test
    public void testMultiValuePositionalParamWithArray() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(int[].class).build();
        assertTrue(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("1", "2", "3");
        assertArrayEquals(new int[] {1, 2, 3}, (int[]) spec.positionalParameters().get(0).getValue());
    }

    @Test
    public void testMultiValueOptionWithListAndAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(List.class).auxiliaryTypes(Integer.class).build();
        assertTrue(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "1", "2", "3");
        assertEquals(Arrays.asList(1, 2, 3), spec.optionsMap().get("-c").getValue());
    }

    @Test
    public void testMultiValuePositionalParamWithListAndAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(List.class).auxiliaryTypes(Integer.class).build();
        assertTrue(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("1", "2", "3");
        assertEquals(Arrays.asList(1, 2, 3), spec.positionalParameters().get(0).getValue());
    }

    @Test
    public void testMultiValueOptionWithListWithoutAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(List.class).build();
        assertTrue(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "1", "2", "3");
        assertEquals(Arrays.asList("1", "2", "3"), spec.optionsMap().get("-c").getValue());
    }

    @Test
    public void testMultiValuePositionalParamWithListWithoutAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(List.class).build();
        assertTrue(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("1", "2", "3");
        assertEquals(Arrays.asList("1", "2", "3"), spec.positionalParameters().get(0).getValue());
    }

    @Test
    public void testMultiValueOptionWithMapAndAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(Map.class).auxiliaryTypes(Integer.class, Double.class).build();
        assertTrue(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "1=1.0", "2=2.0", "3=3.0");
        Map<Integer, Double> expected = new LinkedHashMap<Integer, Double>();
        expected.put(1, 1.0);
        expected.put(2, 2.0);
        expected.put(3, 3.0);
        assertEquals(expected, spec.optionsMap().get("-c").getValue());
    }

    @Test
    public void testMultiValuePositionalParamWithMapAndAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(Map.class).auxiliaryTypes(Integer.class, Double.class).build();
        assertTrue(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("1=1.0", "2=2.0", "3=3.0");
        Map<Integer, Double> expected = new LinkedHashMap<Integer, Double>();
        expected.put(1, 1.0);
        expected.put(2, 2.0);
        expected.put(3, 3.0);
        assertEquals(expected, spec.positionalParameters().get(0).getValue());
    }

    @Test
    public void testMultiValueOptionWithMapWithoutAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        OptionSpec option = OptionSpec.builder("-c", "--count").arity("3").type(Map.class).build();
        assertTrue(option.isMultiValue());

        spec.addOption(option);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "1=1.0", "2=2.0", "3=3.0");
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("1", "1.0");
        expected.put("2", "2.0");
        expected.put("3", "3.0");
        assertEquals(expected, spec.optionsMap().get("-c").getValue());
    }

    @Test
    public void testMultiValuePositionalParamWithMapWithoutAuxTypes() throws Exception {
        CommandSpec spec = CommandSpec.create();
        PositionalParamSpec positional = PositionalParamSpec.builder().index("0").arity("3").type(Map.class).build();
        assertTrue(positional.isMultiValue());

        spec.addPositional(positional);
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("1=1.0", "2=2.0", "3=3.0");
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("1", "1.0");
        expected.put("2", "2.0");
        expected.put("3", "3.0");
        assertEquals(expected, spec.positionalParameters().get(0).getValue());
    }

    @Test
    public void testOptionIsOption() throws Exception {
        assertTrue(OptionSpec.builder("-x").build().isOption());
    }

    @Test
    public void testOptionIsNotPositional() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().isPositional());
    }

    @Test
    public void testPositionalParamSpecIsNotOption() throws Exception {
        assertFalse(PositionalParamSpec.builder().build().isOption());
    }

    @Test
    public void testPositionalParamSpecIsPositional() throws Exception {
        assertTrue(PositionalParamSpec.builder().build().isPositional());
    }

    @Test
    public void testOptionDefaultOrderIsMinusOne() {
        assertEquals(OptionSpec.DEFAULT_ORDER, OptionSpec.builder("-x").build().order());
    }

    @Test
    public void testOptionDefaultUsageHelpIsFalse() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().usageHelp());
    }
    @Test
    public void testOptionDefaultVersionHelpIsFalse() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().versionHelp());
    }
    @Deprecated
    @Test
    public void testOptionDefaultHelpIsFalse() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().help());
    }
    @Test
    public void testOptionDefaultHiddenIsFalse() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().hidden());
    }
    @Test
    public void testPositionalDefaultHiddenIsFalse() throws Exception {
        assertFalse(PositionalParamSpec.builder().build().hidden());
    }
    @Test
    public void testOptionDefaultRequiredIsFalse() throws Exception {
        assertFalse(OptionSpec.builder("-x").build().required());
    }
    @Test
    public void testPositionalDefaultRequiredIsFalse() throws Exception {
        assertFalse(PositionalParamSpec.builder().build().required());
    }
    @Test
    public void testPositionalDefaultFixParamLabelIsFalse() throws Exception {
        assertFalse(PositionalParamSpec.builder().build().hideParamSyntax());
    }

    @Test
    public void testOptionDefaultTypeIsBoolean_withDefaultArity() throws Exception {
        assertEquals(boolean.class, OptionSpec.builder("-x").build().type());
    }

    @Test
    public void testOptionDefaultTypeIsBoolean_withArityZero() throws Exception {
        assertEquals(boolean.class, OptionSpec.builder("-x").arity("0").build().type());
    }

    @Test
    public void testOptionDefaultTypeIsString_withArityOne() throws Exception {
        assertEquals(String.class, OptionSpec.builder("-x").arity("1").build().type());
    }

    @Test
    public void testOptionDefaultTypeIsStringArray_withArityTwo() throws Exception {
        assertEquals(String[].class, OptionSpec.builder("-x").arity("2").build().type());
    }

    @Test
    public void testOptionDefaultAuxiliaryTypesIsDerivedFromType() throws Exception {
        assertArrayEquals(new Class[] {boolean.class}, OptionSpec.builder("-x").build().auxiliaryTypes());
        assertArrayEquals(new Class[] {int.class}, OptionSpec.builder("-x").type(int.class).build().auxiliaryTypes());
    }

    @Test
    public void testOptionDefaultTypDependsOnArity() throws Exception {
        assertEquals(boolean.class, OptionSpec.builder("-x").arity("0").build().type());
        assertEquals(String.class, OptionSpec.builder("-x").arity("1").build().type());
        assertEquals(String.class, OptionSpec.builder("-x").arity("0..1").build().type());
        assertEquals(String[].class, OptionSpec.builder("-x").arity("2").build().type());
        assertEquals(String[].class, OptionSpec.builder("-x").arity("0..2").build().type());
        assertEquals(String[].class, OptionSpec.builder("-x").arity("*").build().type());
    }

    @Test
    public void testOptionAuxiliaryTypeOverridesDefaultType() throws Exception {
        assertEquals(int.class, OptionSpec.builder("-x").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("0").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("1").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("0..1").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("2").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("0..2").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, OptionSpec.builder("-x").arity("*").auxiliaryTypes(int.class).build().type());
    }

    @Test
    public void testPositionalDefaultTypeIsString_withDefaultArity() throws Exception {
        assertEquals(String.class, PositionalParamSpec.builder().build().type());
    }

    @Test
    public void testPositionalDefaultTypeIsString_withArityZero() throws Exception {
        assertEquals(String.class, PositionalParamSpec.builder().arity("0").build().type());
    }

    @Test
    public void testPositionalDefaultTypeIsString_withArityOne() throws Exception {
        assertEquals(String.class, PositionalParamSpec.builder().arity("1").build().type());
    }

    @Test
    public void testPositionalDefaultTypeIsStringArray_withArityTwo() throws Exception {
        assertEquals(String[].class, PositionalParamSpec.builder().arity("2").build().type());
    }

    @Test
    public void testPositionalWithArityHasDefaultTypeString() throws Exception {
        assertEquals(String.class, PositionalParamSpec.builder().arity("0").build().type());
        assertEquals(String.class, PositionalParamSpec.builder().arity("1").build().type());
        assertEquals(String.class, PositionalParamSpec.builder().arity("0..1").build().type());
        assertEquals(String[].class, PositionalParamSpec.builder().arity("2").build().type());
        assertEquals(String[].class, PositionalParamSpec.builder().arity("0..2").build().type());
        assertEquals(String[].class, PositionalParamSpec.builder().arity("*").build().type());
    }

    @Test
    public void testPositionalAuxiliaryTypeOverridesDefaultType() throws Exception {
        assertEquals(int.class, PositionalParamSpec.builder().auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("0").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("1").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("0..1").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("2").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("0..2").auxiliaryTypes(int.class).build().type());
        assertEquals(int.class, PositionalParamSpec.builder().arity("*").auxiliaryTypes(int.class).build().type());
    }
    @Test
    public void testPositionalDefaultAuxiliaryTypesIsDerivedFromType() throws Exception {
        assertArrayEquals(new Class[] {String.class}, PositionalParamSpec.builder().build().auxiliaryTypes());
        assertArrayEquals(new Class[] {int.class}, PositionalParamSpec.builder().type(int.class).build().auxiliaryTypes());
    }

    @Test
    public void testOptionDefaultArityIsZeroIfUntyped() throws Exception {
        assertEquals(Range.valueOf("0"), OptionSpec.builder("-x").build().arity());
    }

    @Test
    public void testOptionDefaultArityIsZeroIfTypeBoolean() throws Exception {
        assertEquals(Range.valueOf("0"), OptionSpec.builder("-x").type(boolean.class).build().arity());
        assertEquals(Range.valueOf("0"), OptionSpec.builder("-x").type(Boolean.class).build().arity());
    }

    @Test
    public void testOptionDefaultArityIsOneIfTypeNonBoolean() throws Exception {
        assertEquals(Range.valueOf("1"), OptionSpec.builder("-x").type(int.class).build().arity());
        assertEquals(Range.valueOf("1"), OptionSpec.builder("-x").type(Integer.class).build().arity());
        assertEquals(Range.valueOf("1"), OptionSpec.builder("-x").type(Byte.class).build().arity());
        assertEquals(Range.valueOf("1"), OptionSpec.builder("-x").type(String.class).build().arity());
    }

    @Test
    public void testPositionalDefaultArityIsOneIfUntyped() throws Exception {
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().build().arity());
    }

    @Test
    public void testPositionalDefaultArityIsOneIfTypeBoolean() throws Exception {
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(boolean.class).build().arity());
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(Boolean.class).build().arity());
    }

    @Test
    public void testPositionalDefaultArityIsOneIfTypeNonBoolean() throws Exception {
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(int.class).build().arity());
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(Integer.class).build().arity());
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(Byte.class).build().arity());
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().type(String.class).build().arity());
    }

    @Test
    public void testOptionDefaultSplitRegexIsEmptyString() throws Exception {
        assertEquals("", OptionSpec.builder("-x").build().splitRegex());
    }
    @Test
    public void testPositionalDefaultSplitRegexIsEmptyString() throws Exception {
        assertEquals("", PositionalParamSpec.builder().build().splitRegex());
    }

    @Test
    public void testOptionDefaultDescriptionIsEmptyArray() throws Exception {
        assertArrayEquals(new String[0], OptionSpec.builder("-x").build().description());
    }
    @Test
    public void testPositionalDefaultDescriptionIsEmptyArray() throws Exception {
        assertArrayEquals(new String[0], PositionalParamSpec.builder().build().description());
    }

    @Test
    public void testOptionDefaultParamLabel() throws Exception {
        assertEquals("PARAM", OptionSpec.builder("-x").build().paramLabel());
    }
    @Test
    public void testPositionalDefaultParamLabel() throws Exception {
        assertEquals("PARAM", PositionalParamSpec.builder().build().paramLabel());
    }

    @Test
    public void testPositionalDefaultIndexIsAll() throws Exception {
        assertEquals(Range.valueOf("*"), PositionalParamSpec.builder().build().index());
    }

    @Test
    public void testPositionalDefaultArityIsOne() throws Exception {
        assertEquals(Range.valueOf("1"), PositionalParamSpec.builder().build().arity());
    }

    @Test
    public void testOptionDefaultConvertersIsEmpty() throws Exception {
        assertArrayEquals(new ITypeConverter[0], OptionSpec.builder("-x").build().converters());
    }
    @Test
    public void testPositionalDefaultConvertersIsEmpty() throws Exception {
        assertArrayEquals(new ITypeConverter[0], PositionalParamSpec.builder().build().converters());
    }

    @Test
    public void testOptionConvertersOverridesRegisteredTypeConverter() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addOption(OptionSpec.builder("-c", "--count").paramLabel("COUNT").arity("1").type(int.class).description("number of times to execute").build());
        spec.addOption(OptionSpec.builder("-s", "--sql").paramLabel("SQLTYPE").type(int.class).converters(
                new CommandLineTypeConversionTest.SqlTypeConverter()).description("sql type converter").build());
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("-c", "33", "-s", "BLOB");
        assertEquals(Integer.valueOf(33), spec.optionsMap().get("-c").getValue());
        assertEquals(Integer.valueOf(Types.BLOB), spec.optionsMap().get("-s").getValue());
    }
    @Test
    public void testPositionalConvertersOverridesRegisteredTypeConverter() throws Exception {
        CommandSpec spec = CommandSpec.create();
        spec.addPositional(PositionalParamSpec.builder().paramLabel("COUNT").index("0").type(int.class).description("number of times to execute").build());
        spec.addPositional(PositionalParamSpec.builder().paramLabel("SQLTYPE").index("1").type(int.class).converters(
                new CommandLineTypeConversionTest.SqlTypeConverter()).description("sql type converter").build());
        CommandLine commandLine = new CommandLine(spec);
        commandLine.parse("33", "BLOB");
        assertEquals(Integer.valueOf(33), spec.positionalParameters().get(0).getValue());
        assertEquals(Integer.valueOf(Types.BLOB), spec.positionalParameters().get(1).getValue());
    }

    @Test
    public void testOptionSpecRequiresNonNullName() throws Exception {
        try {
            OptionSpec.builder(null, "-s").build();
            fail("Expected exception");
        } catch (NullPointerException ex) {
            assertEquals("name", ex.getMessage());
        }
    }

    @Test()
    public void testOptionSpecRequiresNonNullNameArray() throws Exception {
        try {
            OptionSpec.builder(null).build();
            fail("Expected exception");
        } catch (InitializationException ex) {
            assertEquals("OptionSpec names cannot be null. Specify at least one option name.", ex.getMessage());
        }
    }

    @Test
    public void testOptionSpecRequiresAtLeastOneName() throws Exception {
        try {
            OptionSpec.builder(new String[0]).build();
            fail("Expected exception");
        } catch (InitializationException ex) {
            assertEquals("Invalid names: []", ex.getMessage());
        }
    }

    @Test
    public void testOptionSpecRequiresNonEmptyName() throws Exception {
        try {
            OptionSpec.builder("").build();
            fail("Expected exception");
        } catch (InitializationException ex) {
            assertEquals("Invalid names: []", ex.getMessage());
        }
    }

    @Test
    public void testConversion_TODO() {
        // TODO convertion with aux types (abstract field types, generic map with and without explicit type attribute etc)
    }

    @Test
    public void testTypedValues() {
        class App {
            @Option(names="-x") int x;
        }
        ParseResult result1 = new CommandLine(new App()).parseArgs();// not specified
        assertFalse(result1.hasMatchedOption('x'));
        assertTrue(result1.commandSpec().findOption('x').typedValues().isEmpty());

        ParseResult result2 = new CommandLine(new App()).parseArgs("-x", "123");
        assertTrue(result2.hasMatchedOption('x'));
        assertEquals(Integer.valueOf(123), result2.matchedOptionValue('x', 0));

        ParseResult result3 = new CommandLine(new App())
                .setOverwrittenOptionsAllowed(true)
                .parseArgs("-x", "1", "-x", "2", "-x", "3");
        assertTrue(result3.hasMatchedOption('x'));
        assertEquals(Integer.valueOf(3), result3.matchedOptionValue('x', 0));
        assertEquals(Arrays.asList("1", "2", "3"), result3.matchedOption('x').stringValues());
        assertEquals(Arrays.asList(1, 2, 3), result3.matchedOption('x').typedValues());
    }

    /** see <a href="https://github.com/remkop/picocli/issues/279">issue #279</a>  */
    @Test
    public void testSingleValueFieldWithOptionalParameter_279() {
        @Command(name="sample")
        class Sample {
            @Option(names="--foo", arity="0..1") String foo;
        }
        List<CommandLine> parsed1 = new CommandLine(new Sample()).parse();// not specified
        OptionSpec option1 = parsed1.get(0).getCommandSpec().optionsMap().get("--foo");
        assertNull("optional option is null when option not specified", option1.getValue());
        assertTrue("optional option has no string value when option not specified", option1.stringValues().isEmpty());
        assertTrue("optional option has no typed value when option not specified", option1.typedValues().isEmpty());

        List<CommandLine> parsed2 = new CommandLine(new Sample()).parse("--foo");// specified without value
        OptionSpec option2 = parsed2.get(0).getCommandSpec().optionsMap().get("--foo");
        assertEquals("optional option is empty string when specified without args", "", option2.getValue());
        assertEquals("optional option string value when specified without args", "", option2.stringValues().get(0));
        assertEquals("optional option typed value when specified without args", "", option2.typedValues().get(0));

        List<CommandLine> parsed3 = new CommandLine(new Sample()).parse("--foo", "value");// specified with value
        OptionSpec option3 = parsed3.get(0).getCommandSpec().optionsMap().get("--foo");
        assertEquals("optional option is empty string when specified without args", "value", option3.getValue());
        assertEquals("optional option string value when specified without args", "value", option3.stringValues().get(0));
        assertEquals("optional option typed value when specified without args", "value", option3.typedValues().get(0));
    }

    @Test
    public void testOptionBuilderNamesOverwriteInitialValue() {
        OptionSpec option = OptionSpec.builder("-a", "--aaa").names("-b", "--bbb").build();
        assertArrayEquals(new String[] {"-b", "--bbb"}, option.names());
    }

    @Test
    public void testOptionCopyBuilder() {
        OptionSpec option = OptionSpec.builder("-a", "--aaa").arity("1").type(int.class).description("abc").paramLabel("ABC").build();
        OptionSpec copy = option.toBuilder().build();
        assertEquals(option, copy);
        assertNotSame(option, copy);
    }

    @Test
    public void testPositionalCopyBuilder() {
        PositionalParamSpec option = PositionalParamSpec.builder().index("0..34").arity("1").type(int.class).description("abc").paramLabel("ABC").build();
        PositionalParamSpec copy = option.toBuilder().build();
        assertEquals(option, copy);
        assertNotSame(option, copy);
    }

    @Test
    public void testUnmatchedArgsBinding_GetterAndSetterBothNull() {
        try {
            UnmatchedArgsBinding.forStringArrayConsumer(null);
        } catch (IllegalArgumentException ex) {
            assertEquals("Getter and setter cannot both be null", ex.getMessage());
        }
    }

    @Test
    public void testUnmatchedArgsBinding_forStringArrayConsumer() {
        setTraceLevel("OFF");
        class ArrayBinding implements ISetter {
            String[] array;
            @SuppressWarnings("unchecked") public <T> T set(T value) {
                T old = (T) array;
                array = (String[]) value;
                return old;
            }
        }
        ArrayBinding setter = new ArrayBinding();
        CommandSpec cmd = CommandSpec.create();
        UnmatchedArgsBinding unmatched = UnmatchedArgsBinding.forStringArrayConsumer(setter);
        assertSame(setter, unmatched.setter());

        cmd.addUnmatchedArgsBinding(unmatched);
        cmd.addOption(OptionSpec.builder("-x").build());
        ParseResult result = new CommandLine(cmd).parseArgs("-x", "a", "b", "c");

        assertEquals(Arrays.asList("a", "b", "c"), result.unmatched());
        assertArrayEquals(new String[]{"a", "b", "c"}, setter.array);
        assertSame(unmatched, cmd.unmatchedArgsBindings().get(0));
        assertEquals(1, cmd.unmatchedArgsBindings().size());
    }

    @Test
    public void testUnmatchedArgsBinding_forStringCollectionSupplier() {
        setTraceLevel("OFF");
        class ArrayBinding implements IGetter {
            List<String> list = new ArrayList<String>();
            @SuppressWarnings("unchecked") public <T> T get() {
                return (T) list;
            }
        }
        ArrayBinding binding = new ArrayBinding();
        CommandSpec cmd = CommandSpec.create();
        UnmatchedArgsBinding unmatched = UnmatchedArgsBinding.forStringCollectionSupplier(binding);
        assertSame(binding, unmatched.getter());

        cmd.addUnmatchedArgsBinding(unmatched);
        cmd.addOption(OptionSpec.builder("-x").build());
        ParseResult result = new CommandLine(cmd).parseArgs("-x", "a", "b", "c");

        assertEquals(Arrays.asList("a", "b", "c"), result.unmatched());
        assertEquals(Arrays.asList("a", "b", "c"), binding.list);
        assertSame(unmatched, cmd.unmatchedArgsBindings().get(0));
        assertEquals(1, cmd.unmatchedArgsBindings().size());
    }

    @Test
    public void testUnmatchedArgsBinding_forStringArrayConsumer_withInvalidBinding() {
        setTraceLevel("OFF");
        class ListBinding implements ISetter {
            List<String> list = new ArrayList<String>();
            @SuppressWarnings("unchecked") public <T> T set(T value) {
                T old = (T) list;
                list = (List<String>) value;
                return old;
            }
        }
        CommandSpec cmd = CommandSpec.create();
        cmd.addUnmatchedArgsBinding(UnmatchedArgsBinding.forStringArrayConsumer(new ListBinding()));
        try {
            new CommandLine(cmd).parseArgs("-x", "a", "b", "c");
        } catch (Exception ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("Could not invoke setter ("));
            assertTrue(ex.getMessage(), ex.getMessage().contains("with unmatched argument array '[-x, a, b, c]': java.lang.ClassCastException"));
        }
    }

    @Test
    public void testUnmatchedArgsBinding_forStringCollectionSupplier_withInvalidBinding() {
        setTraceLevel("OFF");
        class ListBinding implements IGetter {
            @SuppressWarnings("unchecked") public <T> T get() {
                return (T) new Object();
            }
        }
        CommandSpec cmd = CommandSpec.create();
        cmd.addUnmatchedArgsBinding(UnmatchedArgsBinding.forStringCollectionSupplier(new ListBinding()));
        try {
            new CommandLine(cmd).parseArgs("-x", "a", "b", "c");
        } catch (Exception ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("Could not add unmatched argument array '[-x, a, b, c]' to collection returned by getter ("));
            assertTrue(ex.getMessage(), ex.getMessage().contains("): java.lang.ClassCastException: "));
            assertTrue(ex.getMessage(), ex.getMessage().contains("java.lang.Object"));
        }
    }

    @Test
    public void testUnmatchedArgsBinding_forStringCollectionSupplier_exceptionsRethrownAsPicocliException() {
        class ThrowingGetter implements IGetter {
            public <T> T get() { throw new RuntimeException("test"); }
        }
        try {
            UnmatchedArgsBinding.forStringCollectionSupplier(new ThrowingGetter()).addAll(new String[0]);
            fail("Expected exception");
        } catch (PicocliException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("Could not add unmatched argument array '[]' to collection returned by getter ("));
            assertTrue(ex.getMessage(), ex.getMessage().endsWith("): java.lang.RuntimeException: test"));
        }
    }

    @Test
    public void testUnmatchedArgsBinding_forStringArrayConsumer_exceptionsRethrownAsPicocliException() {
        class ThrowingSetter implements ISetter {
            public <T> T set(T value) { throw new RuntimeException("test"); }
        }
        try {
            UnmatchedArgsBinding.forStringArrayConsumer(new ThrowingSetter()).addAll(new String[0]);
            fail("Expected exception");
        } catch (PicocliException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("Could not invoke setter "));
            assertTrue(ex.getMessage(), ex.getMessage().contains(") with unmatched argument array '[]': java.lang.RuntimeException: test"));
        }
    }

    @Test
    public void testMixinStandardHelpOptions_FalseByDefault() {
        CommandSpec spec = CommandSpec.create();
        assertFalse(spec.mixinStandardHelpOptions());
    }

    @Test
    public void testMixinStandardHelpOptions_SettingToTrueAddsHelpOptions() {
        CommandSpec spec = CommandSpec.create();
        assertTrue(spec.mixins().isEmpty());
        assertTrue(spec.optionsMap().isEmpty());
        assertTrue(spec.posixOptionsMap().isEmpty());
        assertTrue(spec.options().isEmpty());

        spec.mixinStandardHelpOptions(true);
        assertFalse(spec.mixins().isEmpty());
        assertFalse(spec.optionsMap().isEmpty());
        assertFalse(spec.posixOptionsMap().isEmpty());
        assertFalse(spec.options().isEmpty());
        assertTrue(spec.mixinStandardHelpOptions());

        OptionSpec usageHelp = spec.posixOptionsMap().get('h');
        assertSame(usageHelp, spec.optionsMap().get("--help"));
        assertTrue(usageHelp.usageHelp());

        OptionSpec versionHelp = spec.posixOptionsMap().get('V');
        assertSame(versionHelp, spec.optionsMap().get("--version"));
        assertTrue(versionHelp.versionHelp());
    }

    @Test
    public void testMixinStandardHelpOptions_SettingToFalseRemovesHelpOptions() {
        CommandSpec spec = CommandSpec.create();

        spec.mixinStandardHelpOptions(true);
        assertFalse(spec.mixins().isEmpty());
        assertFalse(spec.optionsMap().isEmpty());
        assertFalse(spec.posixOptionsMap().isEmpty());
        assertFalse(spec.options().isEmpty());
        assertTrue(spec.mixinStandardHelpOptions());

        assertNotNull(spec.posixOptionsMap().get('h'));
        assertNotNull(spec.optionsMap().get("--help"));

        assertNotNull(spec.posixOptionsMap().get('V'));
        assertNotNull(spec.optionsMap().get("--version"));

        spec.mixinStandardHelpOptions(false);
        assertTrue(spec.mixins().isEmpty());
        assertTrue(spec.optionsMap().isEmpty());
        assertTrue(spec.posixOptionsMap().isEmpty());
        assertTrue(spec.options().isEmpty());
        assertFalse(spec.mixinStandardHelpOptions());
    }

    @Test
    public void testCommandSpec_forAnnotatedObject_requiresPicocliAnnotation() {
        try {
            CommandSpec.forAnnotatedObject(new Object());
            fail("Expected error");
        } catch (InitializationException ok) {
            assertEquals("java.lang.Object is not a command: it has no @Command, @Option, @Parameters or @Unmatched annotations", ok.getMessage());
        }
    }

    @Test
    public void testCommandSpec_forAnnotatedObjectLenient_doesNotRequirePicocliAnnotation() {
        CommandSpec.forAnnotatedObjectLenient(new Object()); // no error
    }

    @Test
    public void testCommandSpec_forAnnotatedObjectLenient_returnsEmptyCommandSpec() {
        CommandSpec spec = CommandSpec.forAnnotatedObjectLenient(new Object());
        assertTrue(spec.optionsMap().isEmpty());
        assertTrue(spec.posixOptionsMap().isEmpty());
        assertTrue(spec.options().isEmpty());
        assertTrue(spec.positionalParameters().isEmpty());
        assertTrue(spec.unmatchedArgsBindings().isEmpty());
        assertTrue(spec.subcommands().isEmpty());
        assertTrue(spec.mixins().isEmpty());
        assertTrue(spec.requiredArgs().isEmpty());
        assertFalse(spec.mixinStandardHelpOptions());
        assertFalse(spec.helpCommand());
        assertEquals("<main class>", spec.name());
        assertArrayEquals(new String[0], spec.version());
        assertNull(spec.versionProvider());
    }

    @Test
    public void testOptionSpec_setsDefaultValue_ifNotMatched() {
        CommandSpec cmd = CommandSpec.create().addOption(OptionSpec.builder("-x").defaultValue("123").type(int.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs();
        assertFalse(parseResult.hasMatchedOption('x'));
        // TODO this method should be renamed to matchedOptionValue
        assertEquals(Integer.valueOf(-1), parseResult.matchedOptionValue('x', -1));

        // TODO optionValue should return the value of the option, matched or not
        //assertEquals(Integer.valueOf(123), parseResult.optionValue('x'));
        assertEquals(Integer.valueOf(123), parseResult.commandSpec().findOption('x').getValue());
    }

    @Test
    public void testPositionalParamSpec_setsDefaultValue_ifNotMatched() {
        CommandSpec cmd = CommandSpec.create().add(PositionalParamSpec.builder().defaultValue("123").type(int.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs();
        assertFalse(parseResult.hasMatchedPositional(0));
        // TODO this method should be renamed to matchedPositionalValue
        assertEquals(Integer.valueOf(-1), parseResult.matchedPositionalValue(0, -1));

        // TODO positionalValue should return the value of the option, matched or not
        //assertEquals(Integer.valueOf(123), parseResult.positionalValue(0));
        assertEquals(Integer.valueOf(123), parseResult.commandSpec().positionalParameters().get(0).getValue());
    }

    @Test
    public void testOptionSpec_defaultValue_overwritesInitialValue() {
        class Params {
            @Option(names = "-x") int num = 12345;
        }
        CommandLine cmd = new CommandLine(new Params());
        OptionSpec x = cmd.getCommandSpec().posixOptionsMap().get('x').toBuilder().defaultValue("54321").build();

        cmd = new CommandLine(CommandSpec.create().addOption(x));
        ParseResult parseResult = cmd.parseArgs();
        assertFalse(parseResult.hasMatchedOption('x'));
        // TODO this method should be renamed to matchedOptionValue
        assertEquals(Integer.valueOf(-1), parseResult.matchedOptionValue('x', -1));

        // TODO optionValue should return the value of the option, matched or not
        //assertEquals(Integer.valueOf(54321), parseResult.optionValue('x'));
        assertEquals(Integer.valueOf(54321), parseResult.commandSpec().findOption('x').getValue());
    }

    @Test
    public void testPositionalParamSpec_defaultValue_overwritesInitialValue() {
        class Params {
            @Parameters int num = 12345;
        }
        CommandLine cmd = new CommandLine(new Params());
        PositionalParamSpec x = cmd.getCommandSpec().positionalParameters().get(0).toBuilder().defaultValue("54321").build();

        cmd = new CommandLine(CommandSpec.create().add(x));
        ParseResult parseResult = cmd.parseArgs();

        // default not in the parse result
        assertFalse(parseResult.hasMatchedPositional(0));
        assertEquals(Integer.valueOf(-1), parseResult.matchedPositionalValue(0, -1));

        // but positional spec does have the default value
        assertEquals(Integer.valueOf(54321), parseResult.commandSpec().positionalParameters().get(0).getValue());

    }

    @Test
    public void testOptionSpec_notRequiredIfNonNullDefaultValue() {
        assertTrue(OptionSpec.builder("-x").required(true).build().required());
        assertFalse(OptionSpec.builder("-x").defaultValue("123").required(true).build().required());
    }

    @Test
    public void testPositionalParamSpec_notRequiredIfNonNullDefaultValue() {
        assertTrue(PositionalParamSpec.builder().required(true).build().required());
        assertFalse(PositionalParamSpec.builder().defaultValue("123").required(true).build().required());
    }

    @Test
    public void testOptionSpec_DefaultValue_single_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().addOption(OptionSpec.builder("-x").defaultValue("123").type(int.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("-x", "456");
        assertEquals(Integer.valueOf(456), parseResult.matchedOptionValue('x', -1));
    }

    @Test
    public void testPositionalParamSpec_DefaultValue_single_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().add(PositionalParamSpec.builder().defaultValue("123").type(int.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("456");
        assertEquals(Integer.valueOf(456), parseResult.matchedPositionalValue(0, -1));
    }

    @Test
    public void testOptionSpec_DefaultValue_array_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().addOption(OptionSpec
                .builder("-x").defaultValue("1,2,3").splitRegex(",").type(int[].class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("-x", "4,5,6");
        assertArrayEquals(new int[]{4, 5, 6}, parseResult.matchedOptionValue('x', new int[0]));
    }

    @Test
    public void testOptionSpec_DefaultValue_list_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().addOption(OptionSpec
                .builder("-x").defaultValue("1,2,3").splitRegex(",").type(List.class).auxiliaryTypes(Integer.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("-x", "4,5,6");
        assertEquals(Arrays.asList(4, 5, 6), parseResult.matchedOptionValue('x', Collections.emptyList()));
    }

    @Test
    public void testOptionSpec_DefaultValue_map_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().add(OptionSpec
                .builder("-x").defaultValue("1=A,2=B,3=C").splitRegex(",").type(Map.class).auxiliaryTypes(Integer.class, String.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("-x", "4=X,5=Y,6=Z");
        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(4, "X");
        expected.put(5, "Y");
        expected.put(6, "Z");
        assertEquals(expected, parseResult.matchedOptionValue('x', Collections.emptyMap()));
    }

    @Test
    public void testPositionalParamSpec_DefaultValue_array_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().add(PositionalParamSpec
                .builder().defaultValue("1,2,3").splitRegex(",").type(int[].class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("4,5,6");
        assertArrayEquals(new int[]{4, 5, 6}, parseResult.matchedPositionalValue(0, new int[0]));
    }

    @Test
    public void testPositionalParamSpec_DefaultValue_list_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().add(PositionalParamSpec
                .builder().defaultValue("1,2,3").splitRegex(",").type(List.class).auxiliaryTypes(Integer.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("4,5,6");
        assertEquals(Arrays.asList(4, 5, 6), parseResult.matchedPositionalValue(0, Collections.emptyList()));
    }

    @Test
    public void testPositionalParamSpec_DefaultValue_map_replacedByCommandLineValue() {
        CommandSpec cmd = CommandSpec.create().add(PositionalParamSpec
                .builder().defaultValue("1=A,2=B,3=C").splitRegex(",").type(Map.class).auxiliaryTypes(Integer.class, String.class).build());

        ParseResult parseResult = new CommandLine(cmd).parseArgs("4=X,5=Y,6=Z");
        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(4, "X");
        expected.put(5, "Y");
        expected.put(6, "Z");
        assertEquals(expected, parseResult.matchedPositionalValue(0, Collections.emptyMap()));
    }

    @Test
    public void testMultipleUsageHelpOptions() {
        setTraceLevel("WARN");
        CommandSpec cmd = CommandSpec.create()
                .add(OptionSpec.builder("-x").type(boolean.class).usageHelp(true).build())
                .add(OptionSpec.builder("-h").type(boolean.class).usageHelp(true).build());

        assertEquals("", systemErrRule.getLog());
        systemErrRule.clearLog();
        new CommandLine(cmd);
        assertEquals("", systemOutRule.getLog());
        assertEquals(String.format("[picocli WARN] Multiple options [-x, -h] are marked as 'usageHelp=true'. Usually a command has only one --help option that triggers display of the usage help message. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.%n"), systemErrRule.getLog());
    }

    @Test
    public void testMultipleVersionHelpOptions() {
        setTraceLevel("WARN");
        CommandSpec cmd = CommandSpec.create()
                .add(OptionSpec.builder("-x").type(boolean.class).versionHelp(true).build())
                .add(OptionSpec.builder("-V").type(boolean.class).versionHelp(true).build());

        assertEquals("", systemErrRule.getLog());
        systemErrRule.clearLog();
        new CommandLine(cmd);
        assertEquals("", systemOutRule.getLog());
        assertEquals(String.format("[picocli WARN] Multiple options [-x, -V] are marked as 'versionHelp=true'. Usually a command has only one --version option that triggers display of the version information. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.%n"), systemErrRule.getLog());
    }

    @Test
    public void testNonBooleanUsageHelpOptions() {
        CommandSpec cmd = CommandSpec.create().add(OptionSpec.builder("-z").type(int.class).usageHelp(true).build());
        try {
            new CommandLine(cmd);
        } catch (InitializationException ex) {
            assertEquals("Non-boolean options like [-z] should not be marked as 'usageHelp=true'. Usually a command has one --help boolean flag that triggers display of the usage help message. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.", ex.getMessage());
        }
    }

    @Test
    public void testNonBooleanVersionHelpOptions() {
        CommandSpec cmd = CommandSpec.create().add(OptionSpec.builder("-x").type(int.class).versionHelp(true).build());
        try {
            new CommandLine(cmd);
        } catch (InitializationException ex) {
            assertEquals("Non-boolean options like [-x] should not be marked as 'versionHelp=true'. Usually a command has one --version boolean flag that triggers display of the version information. Alternatively, consider using @Command(mixinStandardHelpOptions = true) on your command instead.", ex.getMessage());
        }
    }

    @Test
    public void testBooleanObjectUsageHelpOptions() {
        CommandSpec cmd = CommandSpec.create().add(OptionSpec.builder("-z").type(Boolean.class).usageHelp(true).build());
        assertTrue(new CommandLine(cmd).parseArgs("-z").isUsageHelpRequested());
    }

    @Test
    public void testBooleanObjectVersionHelpOptions() {
        CommandSpec cmd = CommandSpec.create().add(OptionSpec.builder("-x").type(Boolean.class).versionHelp(true).build());
        assertTrue(new CommandLine(cmd).parseArgs("-x").isVersionHelpRequested());
    }
    @Test
    @SuppressWarnings("deprecation")
    public void testGettersOnOptionBuilder() {
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception {
                return null;
            }
        };
        IGetter getter = new IGetter() {
            public <T> T get() throws Exception {
                return null;
            }
        };
        ITypeConverter<Integer> converter = new ITypeConverter<Integer>() {
            public Integer convert(String value) throws Exception {
                return null;
            }
        };
        OptionSpec.Builder builder = OptionSpec.builder("-x");
        builder.auxiliaryTypes(Integer.class, Integer.TYPE)
                .type(Double.TYPE)
                .splitRegex(",,,")
                .required(true)
                .defaultValue("DEF")
                .description("Description")
                .paramLabel("param")
                .arity("1")
                .help(true)
                .versionHelp(true)
                .usageHelp(true)
                .order(123)
                .hidden(true)
                .setter(setter)
                .getter(getter)
                .converters(converter)
                .initialValue("ABC")
                .showDefaultValue(Help.Visibility.NEVER)
                .withToString("TOSTRING");
        assertArrayEquals(new Class[]{Integer.class, Integer.TYPE}, builder.auxiliaryTypes());
        assertEquals(Double.TYPE, builder.type());
        assertEquals(",,,", builder.splitRegex());
        assertTrue(builder.required());
        assertEquals("DEF", builder.defaultValue());
        assertArrayEquals(new String[]{"Description"}, builder.description());
        assertEquals("param", builder.paramLabel());
        assertEquals(Range.valueOf("1"), builder.arity());
        assertTrue(builder.help());
        assertTrue(builder.versionHelp());
        assertTrue(builder.usageHelp());
        assertEquals(123, builder.order());
        assertTrue(builder.hidden());
        assertSame(getter, builder.getter());
        assertSame(setter, builder.setter());
        assertSame(converter, builder.converters()[0]);
        assertEquals("ABC", builder.initialValue());
        assertEquals(Help.Visibility.NEVER, builder.showDefaultValue());
        assertEquals("TOSTRING", builder.toString());

        builder.names("a", "b", "c")
                .type(String.class)
                .auxiliaryTypes(StringWriter.class);
        assertArrayEquals(new String[]{"a", "b", "c"}, builder.names());
        assertArrayEquals(new Class[]{StringWriter.class}, builder.auxiliaryTypes());
        assertEquals(String.class, builder.type());
    }

    @Test
    public void testGettersOnPositionalBuilder() {
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception {
                return null;
            }
        };
        IGetter getter = new IGetter() {
            public <T> T get() throws Exception {
                return null;
            }
        };
        ITypeConverter<Integer> converter = new ITypeConverter<Integer>() {
            public Integer convert(String value) throws Exception {
                return null;
            }
        };
        PositionalParamSpec.Builder builder = PositionalParamSpec.builder();
        builder.auxiliaryTypes(Integer.class, Integer.TYPE)
                .type(Double.TYPE)
                .splitRegex(",,,")
                .required(true)
                .defaultValue("DEF")
                .description("Description")
                .paramLabel("param")
                .arity("1")
                .hidden(true)
                .setter(setter)
                .getter(getter)
                .converters(converter)
                .initialValue("ABC")
                .showDefaultValue(Help.Visibility.NEVER)
                .index("3..4")
                .withToString("TOSTRING");
        assertArrayEquals(new Class[]{Integer.class, Integer.TYPE}, builder.auxiliaryTypes());
        assertEquals(Double.TYPE, builder.type());
        assertEquals(",,,", builder.splitRegex());
        assertTrue(builder.required());
        assertEquals("DEF", builder.defaultValue());
        assertArrayEquals(new String[]{"Description"}, builder.description());
        assertEquals("param", builder.paramLabel());
        assertEquals(Range.valueOf("1"), builder.arity());
        assertTrue(builder.hidden());
        assertSame(getter, builder.getter());
        assertSame(setter, builder.setter());
        assertSame(converter, builder.converters()[0]);
        assertEquals("ABC", builder.initialValue());
        assertEquals(Help.Visibility.NEVER, builder.showDefaultValue());
        assertEquals("TOSTRING", builder.toString());
        assertEquals(Range.valueOf("3..4"), builder.index());
    }

    @Test
    public void testParseResetsRawAndOriginalStringValues() {
        CommandSpec spec = CommandSpec.create()
                .addOption(OptionSpec.builder("-x").type(String.class).build())
                .addPositional(PositionalParamSpec.builder().build());
        CommandLine cmd = new CommandLine(spec);
        ParseResult parseResult = cmd.parseArgs("-x", "XVAL", "POSITIONAL");
        assertEquals("XVAL", parseResult.matchedOption('x').getValue());
        assertEquals(Arrays.asList("XVAL"), parseResult.matchedOption('x').stringValues());
        assertEquals(Arrays.asList("XVAL"), parseResult.matchedOption('x').originalStringValues());
        assertEquals("POSITIONAL", parseResult.matchedPositional(0).getValue());
        assertEquals(Arrays.asList("POSITIONAL"), parseResult.matchedPositional(0).stringValues());
        assertEquals(Arrays.asList("POSITIONAL"), parseResult.matchedPositional(0).originalStringValues());

        ParseResult parseResult2 = cmd.parseArgs("-x", "222", "$$$$");
        assertEquals("222", parseResult2.matchedOption('x').getValue());
        assertEquals(Arrays.asList("222"), parseResult2.matchedOption('x').stringValues());
        assertEquals(Arrays.asList("222"), parseResult2.matchedOption('x').originalStringValues());
        assertEquals("$$$$", parseResult2.matchedPositional(0).getValue());
        assertEquals(Arrays.asList("$$$$"), parseResult2.matchedPositional(0).stringValues());
        assertEquals(Arrays.asList("$$$$"), parseResult2.matchedPositional(0).originalStringValues());

    }

    @Test
    public void testInitializingDefaultsShouldNotAddOptionToParseResult() {
        CommandSpec spec = CommandSpec.create()
                .addOption(OptionSpec.builder("-x").type(String.class).defaultValue("xyz").build());
        CommandLine cmd = new CommandLine(spec);
        ParseResult parseResult = cmd.parseArgs();
        assertFalse(parseResult.hasMatchedOption('x'));
    }

    @Test
    public void testInitializingDefaultsShouldNotAddPositionalToParseResult() {
        CommandSpec spec = CommandSpec.create()
                .addPositional(PositionalParamSpec.builder().defaultValue("xyz").build());
        CommandLine cmd = new CommandLine(spec);
        ParseResult parseResult = cmd.parseArgs();
        assertFalse(parseResult.hasMatchedPositional(0));
    }

    @Test
    public void testOptionLongestName_oneName() {
        assertEquals("-x", OptionSpec.builder("-x").build().longestName());
    }

    @Test
    public void testOptionLongestName_multipleEqualLength_returnsFirst() {
        assertEquals("-x", OptionSpec.builder("-x", "-a").build().longestName());
    }

    @Test
    public void testOptionLongestName_returnsLongest() {
        assertEquals("-xxx", OptionSpec.builder("-x", "-xx", "-xxx").build().longestName());
        assertEquals("-aaa", OptionSpec.builder("-x", "-xx", "-aaa").build().longestName());
        assertEquals("-abcd", OptionSpec.builder("-x", "-abcd", "-aaa").build().longestName());
    }

    @Test
    public void testClearArrayOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").arity("2..3").initialValue(new String[] {"ABC"}).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1", "2", "3");
        assertArrayEquals(new String[] {"1", "2", "3"}, (String[]) cmd.findOption("x").getValue());

        cl.parseArgs("-x", "4", "5");
        assertArrayEquals(new String[] {"4", "5"}, (String[]) cmd.findOption("x").getValue());

        cl.parseArgs();
        assertArrayEquals(new String[] {"ABC"}, (String[]) cmd.findOption("x").getValue());
    }

    @Test
    public void testDontClearArrayOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").arity("2..3").initialValue(new String[] {"ABC"}).hasInitialValue(false).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1", "2", "3");
        assertArrayEquals(new String[] {"1", "2", "3"}, (String[]) cmd.findOption("x").getValue());

        cl.parseArgs("-x", "4", "5");
        assertArrayEquals(new String[] {"4", "5"}, (String[]) cmd.findOption("x").getValue());

        cl.parseArgs();
        assertArrayEquals(new String[] {"4", "5"}, (String[]) cmd.findOption("x").getValue());
    }

    @Test
    public void testClearListOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").type(List.class).initialValue(Arrays.asList("ABC")).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1", "-x", "2", "-x", "3");
        assertEquals(Arrays.asList("1", "2", "3"), cmd.findOption("x").getValue());

        cl.parseArgs("-x", "4", "-x", "5");
        assertEquals(Arrays.asList("4", "5"), cmd.findOption("x").getValue());

        cl.parseArgs();
        assertEquals(Arrays.asList("ABC"), cmd.findOption("x").getValue());
    }

    @Test
    public void testDontClearListOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").type(List.class).initialValue(Arrays.asList("ABC")).hasInitialValue(false).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1", "-x", "2", "-x", "3");
        assertEquals(Arrays.asList("1", "2", "3"), cmd.findOption("x").getValue());

        cl.parseArgs("-x", "4", "-x", "5");
        assertEquals(Arrays.asList("4", "5"), cmd.findOption("x").getValue());

        cl.parseArgs();
        assertEquals(Arrays.asList("4", "5"), cmd.findOption("x").getValue());
    }

    @Test
    public void testClearMapOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        Map<String, String> map = new HashMap<String, String>();
        map.put("ABC", "XYZ");
        cmd.addOption(OptionSpec.builder("-x").type(Map.class).initialValue(map).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "A=1", "-x", "B=2", "-x", "C=3");
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("A", "1");
        expected.put("B", "2");
        expected.put("C", "3");
        assertEquals(expected, cmd.findOption("x").getValue());

        cl.parseArgs("-x", "D=4", "-x", "E=5");
        expected = new LinkedHashMap<String, String>();
        expected.put("D", "4");
        expected.put("E", "5");
        assertEquals(expected, cmd.findOption("x").getValue());

        cl.parseArgs();
        assertEquals(map, cmd.findOption("x").getValue());
    }

    @Test
    public void testDontClearMapOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        Map<String, String> map = new HashMap<String, String>();
        map.put("ABC", "XYZ");
        cmd.addOption(OptionSpec.builder("-x").type(Map.class).initialValue(map).hasInitialValue(false).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "A=1", "-x", "B=2", "-x", "C=3");
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("A", "1");
        expected.put("B", "2");
        expected.put("C", "3");
        assertEquals(expected, cmd.findOption("x").getValue());

        cl.parseArgs("-x", "D=4", "-x", "E=5");
        expected = new LinkedHashMap<String, String>();
        expected.put("D", "4");
        expected.put("E", "5");
        assertEquals(expected, cmd.findOption("x").getValue());

        cl.parseArgs();
        assertEquals(expected, cmd.findOption("x").getValue());
    }

    @Test
    public void testClearSimpleOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").type(String.class).initialValue(null).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1");
        assertEquals("1", cmd.findOption("x").getValue());

        cl.parseArgs("-x", "2");
        assertEquals("2", cmd.findOption("x").getValue());

        cl.parseArgs();
        assertNull(cmd.findOption("x").getValue());
    }

    @Test
    public void testDontClearSimpleOptionOldValueBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        cmd.addOption(OptionSpec.builder("-x").type(String.class).initialValue(null).hasInitialValue(false).build());

        CommandLine cl = new CommandLine(cmd);
        cl.parseArgs("-x", "1");
        assertEquals("1", cmd.findOption("x").getValue());

        cl.parseArgs("-x", "2");
        assertEquals("2", cmd.findOption("x").getValue());

        cl.parseArgs();
        assertEquals("2", cmd.findOption("x").getValue());
    }

    @Test
    public void testOptionClearCustomSetterBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        final List<Object> values = new ArrayList<Object>();
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception {
                values.add(value);
                return null;
            }
        };
        cmd.addOption(OptionSpec.builder("-x").type(String.class).setter(setter).build());

        CommandLine cl = new CommandLine(cmd);
        assertTrue(values.isEmpty());
        cl.parseArgs("-x", "1");
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals("1", values.get(1));

        values.clear();
        cl.parseArgs("-x", "2");
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals("2", values.get(1));
    }

    @Test
    public void testPositionalClearCustomSetterBeforeParse() {
        CommandSpec cmd = CommandSpec.create();
        final List<Object> values = new ArrayList<Object>();
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception {
                values.add(value);
                return null;
            }
        };
        cmd.add(PositionalParamSpec.builder().type(String.class).setter(setter).build());

        CommandLine cl = new CommandLine(cmd);
        assertTrue(values.isEmpty());
        cl.parseArgs("1");
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals("1", values.get(1));

        values.clear();
        cl.parseArgs("2");
        assertEquals(2, values.size());
        assertEquals(null, values.get(0));
        assertEquals("2", values.get(1));
    }

    @Test
    public void test381_NPE_whenAddingSubcommand() {
        CommandSpec toplevel = CommandSpec.create();
        toplevel.addOption(OptionSpec.builder("-o").description("o option").build());

        CommandSpec sub = CommandSpec.create();
        sub.addOption(OptionSpec.builder("-x").description("x option").build());

        CommandLine commandLine = new CommandLine(toplevel);
        commandLine.addSubcommand("sub", sub); // NPE here
        commandLine.usage(System.out);

        String expected = String.format("" +
                "Usage: <main class> [-o] [COMMAND]%n" +
                "  -o     o option%n" +
                "Commands:%n" +
                "  sub%n");
        assertEquals(expected, systemOutRule.getLog());
    }

    @Test
    public void testSubcommandNameIsInitializedWhenAddedToParent() {
        CommandSpec toplevel = CommandSpec.create();
        toplevel.addOption(OptionSpec.builder("-o").description("o option").build());

        CommandSpec sub = CommandSpec.create();
        sub.addOption(OptionSpec.builder("-x").description("x option").build());

        CommandLine commandLine = new CommandLine(toplevel);
        CommandLine subCommandLine = new CommandLine(sub);
        assertEquals("<main class>", sub.name());
        assertEquals("<main class>", subCommandLine.getCommandName());

        commandLine.addSubcommand("sub", subCommandLine);
        assertEquals("sub", sub.name());
        assertEquals("sub", subCommandLine.getCommandName());

        subCommandLine.usage(System.out);

        String expected = String.format("" +
                "Usage: <main class> sub [-x]%n" +
                "  -x     x option%n");
        assertEquals(expected, systemOutRule.getLog());
    }

    @Test
    public void testSubcommandNameNotOverwrittenWhenAddedToParent() {
        CommandSpec toplevel = CommandSpec.create();
        toplevel.addOption(OptionSpec.builder("-o").description("o option").build());

        CommandSpec sub = CommandSpec.create().name("SOMECOMMAND");
        sub.addOption(OptionSpec.builder("-x").description("x option").build());

        CommandLine commandLine = new CommandLine(toplevel);
        CommandLine subCommandLine = new CommandLine(sub);
        assertEquals("SOMECOMMAND", sub.name());
        assertEquals("SOMECOMMAND", subCommandLine.getCommandName());

        commandLine.addSubcommand("sub", subCommandLine);
        assertEquals("SOMECOMMAND", sub.name());
        assertEquals("SOMECOMMAND", subCommandLine.getCommandName());

        subCommandLine.usage(System.out);

        String expected = String.format("" +
                "Usage: <main class> SOMECOMMAND [-x]%n" +
                "  -x     x option%n");
        assertEquals(expected, systemOutRule.getLog());
    }

    @Test
    public void testInject_AnnotatedFieldInjected() {
        class Injected {
            @Spec CommandSpec commandSpec;
            @Parameters String[] params;
        }
        Injected injected = new Injected();
        assertNull(injected.commandSpec);

        CommandLine cmd = new CommandLine(injected);
        assertSame(cmd.getCommandSpec(), injected.commandSpec);
    }

    @Test
    public void testInject_AnnotatedFieldInjectedForSubcommand() {
        class Injected {
            @Spec CommandSpec commandSpec;
            @Parameters String[] params;
        }
        Injected injected = new Injected();
        Injected sub = new Injected();

        assertNull(injected.commandSpec);
        assertNull(sub.commandSpec);

        CommandLine cmd = new CommandLine(injected);
        assertSame(cmd.getCommandSpec(), injected.commandSpec);

        CommandLine subcommand = new CommandLine(sub);
        assertSame(subcommand.getCommandSpec(), sub.commandSpec);
    }

    @Test
    public void testInject_FieldMustBeCommandSpec() {
        class Injected {
            @Spec CommandLine commandLine;
            @Parameters String[] params;
        }
        Injected injected = new Injected();
        try {
            new CommandLine(injected);
            fail("Expect exception");
        } catch (InitializationException ex) {
            assertEquals("@picocli.CommandLine.Spec annotation is only supported on fields of type picocli.CommandLine$Model$CommandSpec", ex.getMessage());
        }
    }

    @Test
    public void testOptionInteractiveFalseByDefault() {
        assertFalse(OptionSpec.builder("-x").interactive());
        assertFalse(OptionSpec.builder("-x").build().interactive());
    }

    @Test
    public void testOptionInteractiveIfSet() {
        assertTrue(OptionSpec.builder("-x").interactive(true).interactive());
        assertTrue(OptionSpec.builder("-x").arity("1").interactive(true).build().interactive());
    }

    @Test
    public void testPositionalInteractiveFalseByDefault() {
        assertFalse(PositionalParamSpec.builder().interactive());
        assertFalse(PositionalParamSpec.builder().build().interactive());
    }

    @Test
    public void testPositionalInteractiveIfSet() {
        assertTrue(PositionalParamSpec.builder().interactive(true).interactive());
        assertTrue(PositionalParamSpec.builder().interactive(true).build().interactive());
    }

    @Test
    public void testOptionInteractiveReadFromAnnotation() {
        class App {
            @Option(names = "-x", interactive = true) int x;
            @Option(names = "-y", interactive = false) int y;
            @Option(names = "-z") int z;
        }

        CommandLine cmd = new CommandLine(new App());
        assertTrue(cmd.getCommandSpec().findOption("x").interactive());
        assertFalse(cmd.getCommandSpec().findOption("y").interactive());
        assertFalse(cmd.getCommandSpec().findOption("z").interactive());
    }

    @Test
    public void testOptionInteractiveNotSupportedForMultiValue() {
        OptionSpec.Builder[] options = new OptionSpec.Builder[]{
                OptionSpec.builder("-x").arity("0").interactive(true),
                OptionSpec.builder("-x").arity("0..1").interactive(true),
                OptionSpec.builder("-x").arity("2").interactive(true),
                OptionSpec.builder("-x").arity("3").interactive(true),
                OptionSpec.builder("-x").arity("1..2").interactive(true),
                OptionSpec.builder("-x").arity("1..*").interactive(true),
                OptionSpec.builder("-x").arity("0..*").interactive(true),
        };
        for (OptionSpec.Builder opt : options) {
            try {
                opt.build();
                fail("Expected exception");
            } catch (InitializationException ex) {
                assertEquals("Interactive options and positional parameters are only supported for arity=1, not for arity=" + opt.arity(), ex.getMessage());
            }
        }
    }

    @Test
    public void testPositionalInteractiveNotSupportedForMultiValue() {
        PositionalParamSpec.Builder[] options = new PositionalParamSpec.Builder[]{
                PositionalParamSpec.builder().arity("0").interactive(true),
                PositionalParamSpec.builder().arity("0..1").interactive(true),
                PositionalParamSpec.builder().arity("2").interactive(true),
                PositionalParamSpec.builder().arity("3").interactive(true),
                PositionalParamSpec.builder().arity("1..2").interactive(true),
                PositionalParamSpec.builder().arity("1..*").interactive(true),
                PositionalParamSpec.builder().arity("0..*").interactive(true),
        };
        for (PositionalParamSpec.Builder opt : options) {
            try {
                opt.build();
                fail("Expected exception");
            } catch (InitializationException ex) {
                assertEquals("Interactive options and positional parameters are only supported for arity=1, not for arity=" + opt.arity(), ex.getMessage());
            }
        }
    }

    @Test
    public void testPositionalInteractiveReadFromAnnotation() {
        class App {
            @Parameters(index = "0", interactive = true) int x;
            @Parameters(index = "1", interactive = false) int y;
            @Parameters(index = "2") int z;
        }

        CommandLine cmd = new CommandLine(new App());
        assertTrue(cmd.getCommandSpec().positionalParameters().get(0).interactive());
        assertFalse(cmd.getCommandSpec().positionalParameters().get(1).interactive());
        assertFalse(cmd.getCommandSpec().positionalParameters().get(2).interactive());
    }

    @Test
    public void testCommandSpecQualifiedName_topLevelCommand() {
        CommandLine cmd = new CommandLine(new I18nCommand());
        assertEquals("i18n-top", cmd.getCommandSpec().qualifiedName("."));
        Map<String, CommandLine> subcommands = cmd.getSubcommands();
        assertEquals("i18n-top.help", subcommands.get("help").getCommandSpec().qualifiedName("."));

        CommandLine sub = subcommands.get("i18n-sub");
        assertEquals("i18n-top.i18n-sub", sub.getCommandSpec().qualifiedName("."));
        assertEquals("i18n-top.i18n-sub.help", sub.getSubcommands().get("help").getCommandSpec().qualifiedName("."));
    }

    @Test
    public void testCommandSpecParserSetter() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        ParserSpec old = spec.parser();
        assertSame(old, spec.parser());
        assertFalse(spec.parser().collectErrors());
        assertFalse(spec.parser().caseInsensitiveEnumValuesAllowed());

        ParserSpec update = new ParserSpec().collectErrors(true).caseInsensitiveEnumValuesAllowed(true);
        spec.parser(update);
        assertSame(old, spec.parser());
        assertTrue(spec.parser().collectErrors());
        assertTrue(spec.parser().caseInsensitiveEnumValuesAllowed());
    }

    @Test
    public void testCommandSpecUsageMessageSetter() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        UsageMessageSpec old = spec.usageMessage();
        assertSame(old, spec.usageMessage());
        assertArrayEquals(new String[0], spec.usageMessage().description());

        UsageMessageSpec update = new UsageMessageSpec().description("hi");
        spec.usageMessage(update);
        assertSame(old, spec.usageMessage());
        assertArrayEquals(new String[] {"hi"}, spec.usageMessage().description());
    }

    @Test
    public void testCommandSpecAddSubcommand_DisallowsDuplicateSubcommandNames() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        CommandSpec sub = CommandSpec.wrapWithoutInspection(null);

        spec.addSubcommand("a", new CommandLine(sub));
        try {
            spec.addSubcommand("a", new CommandLine(sub));
        } catch (InitializationException ex) {
            assertEquals("Another subcommand named 'a' already exists for command '<main class>'", ex.getMessage());
        }
    }

    @Test
    public void testCommandSpecAddSubcommand_DisallowsDuplicateSubcommandAliases() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        CommandSpec sub = CommandSpec.wrapWithoutInspection(null);

        spec.addSubcommand("a", new CommandLine(sub));

        CommandSpec sub2 = CommandSpec.wrapWithoutInspection(null);
        sub2.aliases("a");
        try {
            spec.addSubcommand("x", new CommandLine(sub2));
        } catch (InitializationException ex) {
            assertEquals("Alias 'a' for subcommand 'x' is already used by another subcommand of '<main class>'", ex.getMessage());
        }
    }

    @Test
    public void testCommandSpecAddSubcommand_SubcommandInheritsResourceBundle() {
        ResourceBundle rb = ResourceBundle.getBundle("picocli.SharedMessages");
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.resourceBundle(rb);
        assertSame(rb, spec.resourceBundle());

        CommandSpec sub = CommandSpec.wrapWithoutInspection(null);
        spec.addSubcommand("a", new CommandLine(sub));

        assertSame(rb, sub.resourceBundle());
    }

    @Test
    public void testAliasesWithEmptyArray() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        assertArrayEquals(new String[0], spec.aliases());
        spec.aliases(null);
        assertArrayEquals(new String[0], spec.aliases());
    }

    @Test
    public void testInitHelpCommand() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        assertFalse(spec.helpCommand());

        CommandSpec mixin = CommandSpec.wrapWithoutInspection(null);
        mixin.helpCommand(true);

        spec.addMixin("helper", mixin);
        assertTrue(spec.helpCommand());
    }

    @Test
    public void testInitVersionProvider() {
        IVersionProvider versionProvider1 = new IVersionProvider() {
            public String[] getVersion() { return new String[0]; }
        };
        IVersionProvider versionProvider2 = new IVersionProvider() {
            public String[] getVersion() { return new String[0];  }
        };

        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.versionProvider(versionProvider1);

        CommandSpec mixin = CommandSpec.wrapWithoutInspection(null);
        mixin.versionProvider(versionProvider2);

        spec.addMixin("helper", mixin);
        assertSame(versionProvider1, spec.versionProvider());
    }

    @Test
    public void testDefaultValueProvider() {
        IDefaultValueProvider provider1 = new IDefaultValueProvider() {
            public String defaultValue(ArgSpec argSpec) { return null; }
        };
        IDefaultValueProvider provider2 = new IDefaultValueProvider() {
            public String defaultValue(ArgSpec argSpec) { return null; }
        };

        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.defaultValueProvider(provider1);

        CommandSpec mixin = CommandSpec.wrapWithoutInspection(null);
        mixin.defaultValueProvider(provider2);

        spec.addMixin("helper", mixin);
        assertSame(provider1, spec.defaultValueProvider());
    }

    @Test
    public void testResemblesOption_WhenUnmatchedArePositional() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.parser().unmatchedOptionsArePositionalParams(true);
        assertFalse(spec.resemblesOption("blah", null));

        System.setProperty("picocli.trace", "DEBUG");
        Tracer tracer = new Tracer();
        System.clearProperty("picocli.trace");
        assertFalse(spec.resemblesOption("blah", tracer));

        Tracer tracer2 = new Tracer();
        assertFalse(spec.resemblesOption("blah", tracer2));
    }

    @Test
    public void testResemblesOption_WithoutOptions() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.parser().unmatchedOptionsArePositionalParams(false);
        assertFalse(spec.resemblesOption("blah", null));

        System.setProperty("picocli.trace", "DEBUG");
        Tracer tracer = new Tracer();
        System.clearProperty("picocli.trace");
        assertFalse(spec.resemblesOption("blah", tracer));
        assertTrue(spec.resemblesOption("-a", tracer));

        Tracer tracer2 = new Tracer();
        assertFalse(spec.resemblesOption("blah", tracer2));
        assertTrue(spec.resemblesOption("-a", tracer));
    }

    @Test
    public void testResemblesOption_WithOptionsDash() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.addOption(OptionSpec.builder("-x").build());

        spec.parser().unmatchedOptionsArePositionalParams(false);
        assertFalse(spec.resemblesOption("blah", null));

        System.setProperty("picocli.trace", "DEBUG");
        Tracer tracer = new Tracer();
        System.clearProperty("picocli.trace");
        assertFalse(spec.resemblesOption("blah", tracer));
        assertTrue(spec.resemblesOption("-a", tracer));
        assertFalse(spec.resemblesOption("/a", tracer));

        Tracer tracer2 = new Tracer();
        assertFalse(spec.resemblesOption("blah", tracer2));
        assertTrue(spec.resemblesOption("-a", tracer));
        assertFalse(spec.resemblesOption("/a", tracer));
    }

    @Test
    public void testResemblesOption_WithOptionsNonDash() {
        CommandSpec spec = CommandSpec.wrapWithoutInspection(null);
        spec.addOption(OptionSpec.builder("/x").build());

        spec.parser().unmatchedOptionsArePositionalParams(false);
        assertFalse(spec.resemblesOption("blah", null));

        System.setProperty("picocli.trace", "DEBUG");
        Tracer tracer = new Tracer();
        System.clearProperty("picocli.trace");
        assertFalse(spec.resemblesOption("blah", tracer));
        assertFalse(spec.resemblesOption("-a", tracer));
        assertTrue(spec.resemblesOption("/a", tracer));

        Tracer tracer2 = new Tracer();
        assertFalse(spec.resemblesOption("blah", tracer2));
        assertFalse(spec.resemblesOption("-a", tracer));
        assertTrue(spec.resemblesOption("/a", tracer));
    }

    @Test
    public void testUsageSpec_CustomSynopsisSetter() {
        UsageMessageSpec usage = new UsageMessageSpec();
        assertArrayEquals(new String[0], usage.customSynopsis());

        usage.customSynopsis("abc", "def");
        assertArrayEquals(new String[] {"abc", "def"}, usage.customSynopsis());
    }

    @Test
    public void testUsageSpec_HiddenSetter() {
        UsageMessageSpec usage = new UsageMessageSpec();
        assertFalse(usage.hidden());

        usage.hidden(true);
        assertTrue(usage.hidden());
    }

    @Test
    public void testUsageSpec_commandListHeading() {
        UsageMessageSpec usage = new UsageMessageSpec();
        assertEquals("Commands:%n", usage.commandListHeading());

        usage.commandListHeading("abcdef");
        assertEquals("abcdef", usage.commandListHeading());
    }

    @Test
    public void testUsageSpec_InitFromMixin() {
        UsageMessageSpec usage = new UsageMessageSpec();
        assertFalse(usage.hidden());

        UsageMessageSpec mixin = new UsageMessageSpec();
        mixin.hidden(true);
        usage.initFromMixin(mixin, null);

        assertTrue(usage.hidden());
    }

    @Test
    public void testArgSpecConstructorWithEmptyAuxTypes() {
        PositionalParamSpec positional = PositionalParamSpec.builder().auxiliaryTypes(new Class[0]).build();
        assertEquals(Range.valueOf("1"), positional.arity());
        assertEquals(String.class, positional.type());
        assertArrayEquals(new Class[] {String.class}, positional.auxiliaryTypes());
    }

    @Test
    public void testArgSpecRenderedDescriptionInitial() {
        PositionalParamSpec positional = PositionalParamSpec.builder().build();
        assertArrayEquals(new String[0], positional.renderedDescription());

        PositionalParamSpec positional2 = PositionalParamSpec.builder().description(new String[0]).build();
        assertArrayEquals(new String[0], positional2.renderedDescription());
    }

    @Test
    public void testArgSpecGetter() {
        IGetter getter = new IGetter() {
            public <T> T get() { return null; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().getter(getter).build();
        assertSame(getter, positional.getter());
    }

    @Test
    public void testArgSpecGetterRethrowsPicocliException() {
        final PicocliException expected = new PicocliException("boom");
        IGetter getter = new IGetter() {
            public <T> T get() { throw expected; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().getter(getter).build();
        try {
            positional.getValue();
        } catch (PicocliException ex) {
            assertSame(expected, ex);
        }
    }

    @Test
    public void testArgSpecGetterWrapNonPicocliException() {
        final Exception expected = new Exception("boom");
        IGetter getter = new IGetter() {
            public <T> T get() throws Exception { throw expected; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().getter(getter).build();
        try {
            positional.getValue();
        } catch (PicocliException ex) {
            assertSame(expected, ex.getCause());
        }
    }

    @Test
    public void testArgSpecSetterRethrowsPicocliException() {
        final PicocliException expected = new PicocliException("boom");
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception { throw expected; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().setter(setter).build();
        try {
            positional.setValue("abc");
        } catch (PicocliException ex) {
            assertSame(expected, ex);
        }
    }

    @Test
    public void testArgSpecSetValueCallsSetter() {
        final Object[] newVal = new Object[1];
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception { newVal[0] = value; return null; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().setter(setter).build();
        positional.setValue("abc");
        assertEquals("abc", newVal[0]);
    }

    @Test
    public void testArgSpecSetterWrapNonPicocliException() {
        final Exception expected = new Exception("boom");
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception { throw expected; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().setter(setter).build();
        try {
            positional.setValue("abc");
        } catch (PicocliException ex) {
            assertSame(expected, ex.getCause());
        }
    }

    @Test
    public void testArgSpecSetter2WrapNonPicocliException() {
        final Exception expected = new Exception("boom");
        ISetter setter = new ISetter() {
            public <T> T set(T value) throws Exception { throw expected; }
        };
        PositionalParamSpec positional = PositionalParamSpec.builder().setter(setter).build();
        try {
            positional.setValue("abc", new CommandLine(CommandSpec.create()));
        } catch (PicocliException ex) {
            assertSame(expected, ex.getCause());
        }
    }

    @Test
    public void testArgSpecSplitValueDebug() {
        PositionalParamSpec positional = PositionalParamSpec.builder().splitRegex("b").build();

        System.setProperty("picocli.trace", "DEBUG");
        String[] values = positional.splitValue("abc", new ParserSpec().splitQuotedStrings(true), Range.valueOf("1"), 1);
        System.clearProperty("picocli.trace");

        assertArrayEquals(new String[] {"a", "c"}, values);
    }

    @Test
    public void testArgSpecSplitBalancedQuotedValueDebug() {
        PositionalParamSpec positional = PositionalParamSpec.builder().splitRegex(";").build();

        System.setProperty("picocli.trace", "DEBUG");
        String value = "\"abc\\\";def\"";
        String[] values = positional.splitValue(value, new ParserSpec().splitQuotedStrings(false), Range.valueOf("1"), 1);
        System.clearProperty("picocli.trace");

        assertArrayEquals(new String[] {"\"abc\\\";def\""}, values);
    }

    @Test
    public void testArgSpecSplitUnbalancedQuotedValueDebug() {
        PositionalParamSpec positional = PositionalParamSpec.builder().splitRegex(";").build();

        System.setProperty("picocli.trace", "DEBUG");
        String value = "\"abc\\\";def";
        String[] values = positional.splitValue(value, new ParserSpec().splitQuotedStrings(false), Range.valueOf("1"), 1);
        System.clearProperty("picocli.trace");

        assertArrayEquals(new String[] {"\"abc\\\"", "def"}, values);
    }

    @Test
    public void testOptionSpecEquals() {
        OptionSpec.Builder option = OptionSpec.builder("-x")
                .arity("1")
                .hideParamSyntax(true)
                .required(true)
                .splitRegex(";")
                .description("desc")
                .descriptionKey("key")
                .auxiliaryTypes(Integer.class, Double.class)
                .help(true)
                .usageHelp(true)
                .versionHelp(true)
                .order(123);

        OptionSpec p1 = option.build();
        assertEquals(p1, p1);
        assertEquals(p1, option.build());
        assertNotEquals(p1, option.arity("2").build());
        assertNotEquals(p1, option.arity("1").hideParamSyntax(false).build());
        assertNotEquals(p1, option.hideParamSyntax(true).required(false).build());
        assertNotEquals(p1, option.required(true).splitRegex(",").build());
        assertNotEquals(p1, option.splitRegex(";").description("xyz").build());
        assertNotEquals(p1, option.description("desc").descriptionKey("XX").build());
        assertNotEquals(p1, option.descriptionKey("key").auxiliaryTypes(Short.class).build());
        assertEquals(p1, option.auxiliaryTypes(Integer.class, Double.class).build());

        assertNotEquals(p1, option.help(false).build());
        assertNotEquals(p1, option.help(true).usageHelp(false).build());
        assertNotEquals(p1, option.usageHelp(true).versionHelp(false).build());
        assertNotEquals(p1, option.versionHelp(true).order(999).build());
        assertNotEquals(p1, option.order(123).names("-a", "-b", "-c").build());
        assertEquals(p1, option.names("-x").build());
    }

    @Test
    public void testArgSpecEquals() {
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .arity("1")
                .hideParamSyntax(true)
                .required(true)
                .splitRegex(";")
                .description("desc")
                .descriptionKey("key")
                .auxiliaryTypes(Integer.class, Double.class);

        PositionalParamSpec p1 = positional.build();
        assertEquals(p1, p1);
        assertEquals(p1, positional.build());
        assertNotEquals(p1, positional.arity("2").build());
        assertNotEquals(p1, positional.arity("1").hideParamSyntax(false).build());
        assertNotEquals(p1, positional.hideParamSyntax(true).required(false).build());
        assertNotEquals(p1, positional.required(true).splitRegex(",").build());
        assertNotEquals(p1, positional.splitRegex(";").description("xyz").build());
        assertNotEquals(p1, positional.description("desc").descriptionKey("XX").build());
        assertNotEquals(p1, positional.descriptionKey("key").auxiliaryTypes(Short.class).build());
        assertEquals(p1, positional.auxiliaryTypes(Integer.class, Double.class).build());
    }

    @Test
    public void testPositionalParamSpecEquals() {
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .arity("1")
                .hideParamSyntax(true)
                .required(true)
                .splitRegex(";")
                .description("desc")
                .descriptionKey("key")
                .auxiliaryTypes(Integer.class, Double.class)
                .index("1..3");

        PositionalParamSpec p1 = positional.build();
        assertEquals(p1, p1);
        assertEquals(p1, positional.build());
        assertNotEquals(p1, positional.arity("2").build());
        assertNotEquals(p1, positional.arity("1").hideParamSyntax(false).build());
        assertNotEquals(p1, positional.hideParamSyntax(true).required(false).build());
        assertNotEquals(p1, positional.required(true).splitRegex(",").build());
        assertNotEquals(p1, positional.splitRegex(";").description("xyz").build());
        assertNotEquals(p1, positional.description("desc").descriptionKey("XX").build());
        assertNotEquals(p1, positional.descriptionKey("key").auxiliaryTypes(Short.class).build());
        assertEquals(p1, positional.auxiliaryTypes(Integer.class, Double.class).build());

        assertNotEquals(p1, positional.index("0..*").build());
        assertEquals(p1, positional.index("1..3").build());
    }

    @Test
    public void testArgSpecBuilderDescriptionKey() {
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .descriptionKey("key");

        assertEquals("key", positional.descriptionKey());
        assertEquals("xxx", positional.descriptionKey("xxx").descriptionKey());
    }

    @Test
    public void testArgSpecBuilderHideParamSyntax() {
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .hideParamSyntax(true);

        assertEquals(true, positional.hideParamSyntax());
        assertEquals(false, positional.hideParamSyntax(false).hideParamSyntax());
    }

    @Test
    public void testArgSpecBuilderHasInitialValue() {
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .hasInitialValue(true);

        assertEquals(true, positional.hasInitialValue());
        assertEquals(false, positional.hasInitialValue(false).hasInitialValue());
    }

    @Test
    public void testArgSpecBuilderCompletionCandidates() {
        List<String> candidates = Arrays.asList("a", "b");
        PositionalParamSpec.Builder positional = PositionalParamSpec.builder()
                .completionCandidates(candidates);

        assertEquals(candidates, positional.completionCandidates());
    }

    @Test
    public void testMessagesCopyNull() {
        assertNull(Messages.copy(CommandSpec.create(), null));
    }

    @Test
    public void testMessagesCopyNonNull() {
        Messages orig = new Messages(CommandSpec.create(), null);
        Messages copy = Messages.copy(CommandSpec.create(), orig);
        assertNull(copy.resourceBundle());
    }

    @Test
    public void testMessagesCommandSpec() {
        CommandSpec spec = CommandSpec.create();
        Messages orig = new Messages(spec, null);
        assertSame(spec, orig.commandSpec());
    }

    @Test
    public void testMessagesEmpty() {
        assertTrue(Messages.empty((Messages) null));
        assertTrue(Messages.empty(new Messages(CommandSpec.create(), null)));
    }

    @Test
    public void testMessagesGetStringNullKey() {
        String def = "abc";
        assertSame(def, new Messages(CommandSpec.create(), null).getString(null, def));
        assertSame(def, new Messages(CommandSpec.create(), null).getString("help", def));

        ResourceBundle rb = ResourceBundle.getBundle("picocli.SharedMessages");
        assertSame(def, new Messages(CommandSpec.create(), rb).getString(null, def));

        assertNotEquals(def, new Messages(CommandSpec.create(), rb).getString("help", def));
    }

    @Test
    public void testMessagesGetStringArrayNullKey() {
        String[] def = {"abc"};
        assertSame(def, new Messages(CommandSpec.create(), null).getStringArray(null, def));
        assertSame(def, new Messages(CommandSpec.create(), null).getStringArray("help", def));

        ResourceBundle rb = ResourceBundle.getBundle("picocli.SharedMessages");
        assertSame(def, new Messages(CommandSpec.create(), rb).getStringArray(null, def));

        assertNotEquals(def, new Messages(CommandSpec.create(), rb).getStringArray("usage.description", def));
    }

    @Test
    public void testCommandReflection() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method extractCommandSpec = reflection.getDeclaredMethod("extractCommandSpec", Object.class, IFactory.class, boolean.class);

        IFactory myFactory = new IFactory() {
            public <K> K create(Class<K> cls) throws Exception {
                throw new InitializationException("boom");
            }
        };

        try {
            extractCommandSpec.invoke(null, Object.class, myFactory, false);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            InitializationException ex = (InitializationException) ite.getCause();
            assertEquals("Could not instantiate class java.lang.Object: picocli.CommandLine$InitializationException: boom", ex.getMessage());
        }
    }

    @Command(subcommands = InvalidSub.class)
    static class InvalidTop {}

    @Command(name = "invalidsub")
    static class InvalidSub {
        public InvalidSub(int x) {}
    }

    @Test
    public void testCommandReflection_initSubcommands() throws Exception {
        try {
            CommandSpec.forAnnotatedObject(InvalidTop.class);
        } catch (InitializationException ex) {
            assertEquals("Cannot instantiate subcommand picocli.CommandLineModelTest$InvalidSub: the class has no constructor", ex.getMessage());
        }
    }

    @Command(subcommands = InvalidSub2.class)
    static class InvalidTop2 {}

    static class InvalidSub2 {
        @Option(names = "-x") int x;
    }

    @Test
    public void testCommandReflection_subcommandName() throws Exception {
        try {
            CommandSpec.forAnnotatedObject(InvalidTop2.class);
        } catch (InitializationException ex) {
            assertEquals("Subcommand picocli.CommandLineModelTest$InvalidSub2 is missing the mandatory @Command annotation with a 'name' attribute", ex.getMessage());
        }
    }

    static class TypedMemberObj {
        int x;
    }
    @Test
    public void testCommandReflection_validateMixin() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateMixin = reflection.getDeclaredMethod("validateMixin", TypedMember.class);
        validateMixin.setAccessible(true);

        TypedMember typedMember = new TypedMember(TypedMemberObj.class.getDeclaredField("x"));
        try {
            validateMixin.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            IllegalStateException ex = (IllegalStateException) ite.getCause();
            assertEquals("Bug: validateMixin() should only be called with mixins", ex.getMessage());
        }
    }
    @Test
    public void testCommandReflection_validateUnmatched() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateUnmatched = reflection.getDeclaredMethod("validateUnmatched", TypedMember.class);
        validateUnmatched.setAccessible(true);

        TypedMember typedMember = new TypedMember(TypedMemberObj.class.getDeclaredField("x"));
        validateUnmatched.invoke(null, typedMember); // no error
    }

    static class ValidateArgSpecField {
        @Mixin
        @Option(names = "-x")
        int x;

        @Option(names = "-final")
        final Object f = new Object();

        int neither;
    }

    @Test
    public void testCommandReflection_validateArgSpecField() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateArgSpecField = reflection.getDeclaredMethod("validateArgSpecField", TypedMember.class);
        validateArgSpecField.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateArgSpecField.class.getDeclaredField("x"));
        try {
            validateArgSpecField.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            DuplicateOptionAnnotationsException ex = (DuplicateOptionAnnotationsException) ite.getCause();
            assertEquals("A member cannot be both a @Mixin command and an @Option or @Parameters, but 'int picocli.CommandLineModelTest$ValidateArgSpecField.x' is both.", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_validateArgSpecField_final() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateArgSpecField = reflection.getDeclaredMethod("validateArgSpecField", TypedMember.class);
        validateArgSpecField.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateArgSpecField.class.getDeclaredField("f"));
        validateArgSpecField.invoke(null, typedMember); // no error
    }

    @Ignore
    @Test
    public void testCommandReflection_validateArgSpecField_neither() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateArgSpecField = reflection.getDeclaredMethod("validateArgSpecField", TypedMember.class);
        validateArgSpecField.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateArgSpecField.class.getDeclaredField("neither"));
        validateArgSpecField.invoke(null, typedMember); // no error
    }

    static class ValidateInjectSpec {
        int notAnnotated;

        @Spec
        @Option(names = "-x")
        int x;

        @Spec
        @Parameters
        int y;

        @Spec
        @Unmatched
        List<String> unmatched;

        @Spec
        @Mixin
        Object mixin = new Object();

        @Spec
        Object invalidType;
    }
    @Test
    public void testCommandReflection_ValidateInjectSpec() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("notAnnotated"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            IllegalStateException ex = (IllegalStateException) ite.getCause();
            assertEquals("Bug: validateInjectSpec() should only be called with @Spec members", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_ValidateInjectSpec_Option() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("x"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            DuplicateOptionAnnotationsException ex = (DuplicateOptionAnnotationsException) ite.getCause();
            assertEquals("A member cannot have both @Spec and @Option or @Parameters annotations, but 'int picocli.CommandLineModelTest$ValidateInjectSpec.x' has both.", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_ValidateInjectSpec_Positional() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("y"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            DuplicateOptionAnnotationsException ex = (DuplicateOptionAnnotationsException) ite.getCause();
            assertEquals("A member cannot have both @Spec and @Option or @Parameters annotations, but 'int picocli.CommandLineModelTest$ValidateInjectSpec.y' has both.", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_ValidateInjectSpec_Unmatched() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("unmatched"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            DuplicateOptionAnnotationsException ex = (DuplicateOptionAnnotationsException) ite.getCause();
            assertEquals("A member cannot have both @Spec and @Unmatched annotations, but 'java.util.List picocli.CommandLineModelTest$ValidateInjectSpec.unmatched' has both.", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_ValidateInjectSpec_Mixin() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("mixin"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            DuplicateOptionAnnotationsException ex = (DuplicateOptionAnnotationsException) ite.getCause();
            assertEquals("A member cannot have both @Spec and @Mixin annotations, but 'java.lang.Object picocli.CommandLineModelTest$ValidateInjectSpec.mixin' has both.", ex.getMessage());
        }
    }

    @Test
    public void testCommandReflection_ValidateInjectSpec_FieldType() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateInjectSpec = reflection.getDeclaredMethod("validateInjectSpec", TypedMember.class);
        validateInjectSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("invalidType"));
        try {
            validateInjectSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            InitializationException ex = (InitializationException) ite.getCause();
            assertEquals("@picocli.CommandLine.Spec annotation is only supported on fields of type picocli.CommandLine$Model$CommandSpec", ex.getMessage());
        }
    }
    @Test
    public void testCommandReflection_ValidateArgSpec() throws Exception {
        Class<?> reflection = Class.forName("picocli.CommandLine$Model$CommandReflection");
        Method validateArgSpec = reflection.getDeclaredMethod("validateArgSpecField", TypedMember.class);
        validateArgSpec.setAccessible(true);

        TypedMember typedMember = new TypedMember(ValidateInjectSpec.class.getDeclaredField("notAnnotated"));
        try {
            validateArgSpec.invoke(null, typedMember);
            fail("expected Exception");
        } catch (InvocationTargetException ite) {
            IllegalStateException ex = (IllegalStateException) ite.getCause();
            assertEquals("Bug: validateArgSpecField() should only be called with an @Option or @Parameters member", ex.getMessage());
        }
    }

    @Command(mixinStandardHelpOptions = true)
    static class ValidMixin {
    }
    @Command
    static class MixeeInstantiated {
        @Mixin ValidMixin mixin = new ValidMixin();
    }
    @Command
    static class MixeeUninstantiated {
        @Mixin ValidMixin mixin;
    }

    @Test
    public void testCommandReflection_buildMixinForField_valid() throws Exception {
        CommandSpec commandSpec = CommandSpec.forAnnotatedObject(new MixeeInstantiated());
        assertNotNull(commandSpec.findOption("h"));
    }

    @Test
    public void testCommandReflection_buildMixinForField_invalid() throws Exception {
        IFactory myFactory = new IFactory() {
            public <K> K create(Class<K> cls) throws Exception {
                throw new IllegalStateException("boom");
            }
        };

        try {
            CommandSpec.forAnnotatedObject(new MixeeUninstantiated(), myFactory);
        } catch (InitializationException ex) {
            assertEquals("Could not access or modify mixin member picocli.CommandLineModelTest$ValidMixin picocli.CommandLineModelTest$MixeeUninstantiated.mixin: java.lang.IllegalStateException: boom", ex.getMessage());
        }
    }

    @Command
    class MyUnmatched {
        @CommandLine.Unmatched
        List raw;
    }
    @Test(expected = InitializationException.class)
    public void testCommandReflection_buildUnmatchedForField_raw() throws Exception {
        CommandSpec.forAnnotatedObject(new MyUnmatched());
    }

    @Command
    class MyUnmatched2 {
        @CommandLine.Unmatched
        List<String> raw = new ArrayList<String>();
    }
    @Test
    public void testCommandReflection_buildUnmatchedForField_valid() throws Exception {
        CommandSpec.forAnnotatedObject(new MyUnmatched2());
    }
    @Test
    public void testArgsReflection_inferLabel() throws Exception{
        Method m = ArgsReflection.class.getDeclaredMethod("inferLabel", String.class, String.class, Class.class, Class[].class);
        m.setAccessible(true);
        assertEquals("<String=String>", m.invoke(null, "", "fieldName", Map.class, new Class[0]));
        assertEquals("<String=String>", m.invoke(null, "", "fieldName", Map.class, new Class[]{Integer.class}));
        assertEquals("<String=String>", m.invoke(null, "", "fieldName", Map.class, new Class[]{null, Integer.class}));
        assertEquals("<String=String>", m.invoke(null, "", "fieldName", Map.class, new Class[]{Integer.class, null}));
        assertEquals("<Integer=Integer>", m.invoke(null, "", "fieldName", Map.class, new Class[]{Integer.class, Integer.class}));
    }

    @Test
    public void testArgsReflection_inferTypes() {
        class App {
            @Parameters
            List<Class<? extends Class<? extends String>[]>> list;
        }
        assertEquals("<list>", CommandSpec.forAnnotatedObject(new App()).positionalParameters().get(0).paramLabel());
    }
}
