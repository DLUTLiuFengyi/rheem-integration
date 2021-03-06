package io.rheem.iejoin.operators;

import org.junit.Assert;
import org.junit.Test;
import io.rheem.basic.data.Record;
import io.rheem.basic.data.Tuple2;
import io.rheem.core.function.TransformationDescriptor;
import io.rheem.core.platform.ChannelInstance;
import io.rheem.core.types.DataSetType;
import io.rheem.core.types.DataUnitType;
import io.rheem.spark.channels.RddChannel;

import java.util.Arrays;
import java.util.List;

/**
 * Test suite for {@link SparkIEJoinOperator}.
 */
public class SparkIESelfJoinOperatorTest extends SparkOperatorTestBase {


    @Test
    public void testExecution() {
        //Record r1 = new Record(100, 10);
        Record r2 = new Record(200, 20);
        Record r3 = new Record(300, 30);
        Record r11 = new Record(250, 5);
        // Prepare test data.
        RddChannel.Instance input = this.createRddChannelInstance(Arrays.asList(r2, r3, r11));
        RddChannel.Instance output = this.createRddChannelInstance();

        // Build the Cartesian operator.
        SparkIESelfJoinOperator<Integer, Integer, Record> IESelfJoinOperator =
                new SparkIESelfJoinOperator<Integer, Integer, Record>(
                        DataSetType.createDefaultUnchecked(Record.class),
                        //0, JoinCondition.GreaterThan, 1, JoinCondition.LessThan
                        new TransformationDescriptor<Record, Integer>(word -> (Integer) word.getField(0),
                                DataUnitType.<Record>createBasic(Record.class),
                                DataUnitType.<Integer>createBasicUnchecked(Integer.class)
                        ),
                        IEJoinMasterOperator.JoinCondition.GreaterThan,
                        new TransformationDescriptor<Record, Integer>(word -> (Integer) word.getField(1),
                                DataUnitType.<Record>createBasic(Record.class),
                                DataUnitType.<Integer>createBasicUnchecked(Integer.class)
                        ),
                        IEJoinMasterOperator.JoinCondition.LessThan
                );

        // Set up the ChannelInstances.
        final ChannelInstance[] inputs = new ChannelInstance[]{input};
        final ChannelInstance[] outputs = new ChannelInstance[]{output};

        // Execute.
        evaluate(IESelfJoinOperator, inputs, outputs);

        // Verify the outcome.
        final List<Tuple2<Record, Record>> result = output.<Tuple2<Record, Record>>provideRdd().collect();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(0), new Tuple2<Record, Record>(r11, r2));
        //Assert.assertEquals(result.get(0), new Tuple2(1, "a"));

    }

}
