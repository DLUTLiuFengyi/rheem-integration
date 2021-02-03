package io.rheem.core.mapping;

import org.junit.Assert;
import org.junit.Test;
import io.rheem.core.mapping.test.TestSinkToTestSink2Factory;
import io.rheem.core.plan.rheemplan.Operator;
import io.rheem.core.plan.rheemplan.OperatorAlternative;
import io.rheem.core.plan.rheemplan.RheemPlan;
import io.rheem.core.plan.rheemplan.UnarySink;
import io.rheem.core.plan.rheemplan.UnarySource;
import io.rheem.core.plan.rheemplan.test.TestSink;
import io.rheem.core.plan.rheemplan.test.TestSink2;
import io.rheem.core.plan.rheemplan.test.TestSource;
import io.rheem.core.test.TestDataUnit;
import io.rheem.core.types.DataSetType;

/**
 * Test suite for the {@link io.rheem.core.mapping.PlanTransformation} class.
 */
public class PlanTransformationTest {

    @Test
    public void testReplace() {
        // Build the plan.
        UnarySource source = new TestSource(DataSetType.createDefault(TestDataUnit.class));
        UnarySink sink = new TestSink(DataSetType.createDefault(TestDataUnit.class));
        source.connectTo(0, sink, 0);
        RheemPlan plan = new RheemPlan();
        plan.addSink(sink);

        // Build the pattern.
        OperatorPattern sinkPattern = new OperatorPattern("sink", new TestSink(DataSetType.createDefault(TestDataUnit.class)), false);
        SubplanPattern subplanPattern = SubplanPattern.createSingleton(sinkPattern);

        // Build the replacement strategy.
        ReplacementSubplanFactory replacementSubplanFactory = new TestSinkToTestSink2Factory();
        PlanTransformation planTransformation = new PlanTransformation(subplanPattern, replacementSubplanFactory).thatReplaces();

        // Apply the replacement strategy to the graph.
        planTransformation.transform(plan, Operator.FIRST_EPOCH + 1);

        // Check the correctness of the transformation.
        Assert.assertEquals(1, plan.getSinks().size());
        final Operator replacedSink = plan.getSinks().iterator().next();
        Assert.assertTrue(replacedSink instanceof TestSink2);
        Assert.assertEquals(source, replacedSink.getEffectiveOccupant(0).getOwner());
    }

    @Test
    public void testIntroduceAlternative() {
        // Build the plan.
        UnarySource source = new TestSource(DataSetType.createDefault(TestDataUnit.class));
        UnarySink sink = new TestSink(DataSetType.createDefault(TestDataUnit.class));
        source.connectTo(0, sink, 0);
        RheemPlan plan = new RheemPlan();
        plan.addSink(sink);

        // Build the pattern.
        OperatorPattern sinkPattern = new OperatorPattern("sink", new TestSink(DataSetType.createDefault(TestDataUnit.class)), false);
        SubplanPattern subplanPattern = SubplanPattern.createSingleton(sinkPattern);

        // Build the replacement strategy.
        ReplacementSubplanFactory replacementSubplanFactory = new TestSinkToTestSink2Factory();
        PlanTransformation planTransformation = new PlanTransformation(subplanPattern, replacementSubplanFactory);

        // Apply the replacement strategy to the graph.
        planTransformation.transform(plan, Operator.FIRST_EPOCH + 1);

        // Check the correctness of the transformation.
        Assert.assertEquals(1, plan.getSinks().size());
        final Operator replacedSink = plan.getSinks().iterator().next();
        Assert.assertTrue(replacedSink instanceof OperatorAlternative);
        OperatorAlternative operatorAlternative = (OperatorAlternative) replacedSink;
        Assert.assertEquals(2, operatorAlternative.getAlternatives().size());
        Assert.assertTrue(operatorAlternative.getAlternatives().get(0).getContainedOperator() instanceof TestSink);
        Assert.assertTrue(operatorAlternative.getAlternatives().get(1).getContainedOperator() instanceof TestSink2);
        Assert.assertEquals(source, replacedSink.getEffectiveOccupant(0).getOwner());
    }

    @Test
    public void testFlatAlternatives() {
        // Build the plan.
        UnarySource source = new TestSource(DataSetType.createDefault(TestDataUnit.class));
        UnarySink sink = new TestSink(DataSetType.createDefault(TestDataUnit.class));
        source.connectTo(0, sink, 0);
        RheemPlan plan = new RheemPlan();
        plan.addSink(sink);

        // Build the pattern.
        OperatorPattern sinkPattern = new OperatorPattern("sink", new TestSink(DataSetType.createDefault(TestDataUnit.class)), false);
        SubplanPattern subplanPattern = SubplanPattern.createSingleton(sinkPattern);

        // Build the replacement strategy.
        ReplacementSubplanFactory replacementSubplanFactory = new TestSinkToTestSink2Factory();
        PlanTransformation planTransformation = new PlanTransformation(subplanPattern, replacementSubplanFactory);

        // Apply the replacement strategy to the graph twice.
        planTransformation.transform(plan, Operator.FIRST_EPOCH + 1);
        planTransformation.transform(plan, Operator.FIRST_EPOCH + 1);

        // Check the correctness of the transformation.
        Assert.assertEquals(1, plan.getSinks().size());
        final Operator replacedSink = plan.getSinks().iterator().next();
        Assert.assertTrue(replacedSink instanceof OperatorAlternative);
        OperatorAlternative operatorAlternative = (OperatorAlternative) replacedSink;
        Assert.assertEquals(3, operatorAlternative.getAlternatives().size());
        Assert.assertTrue(operatorAlternative.getAlternatives().get(0).getContainedOperator() instanceof TestSink);
        Assert.assertTrue(operatorAlternative.getAlternatives().get(1).getContainedOperator() instanceof TestSink2);
        Assert.assertTrue(operatorAlternative.getAlternatives().get(2).getContainedOperator() instanceof TestSink2);
        Assert.assertEquals(source, replacedSink.getEffectiveOccupant(0).getOwner());
    }

}