package io.rheem.api

import io.rheem.basic.data.Record
import io.rheem.basic.function.ProjectionDescriptor
import io.rheem.basic.operators.MapOperator
import io.rheem.basic.types.RecordType
import io.rheem.core.optimizer.costs.LoadEstimator

/**
  * This class enhances the functionality of [[DataQuanta]] with [[Record]]s.
  */
class RecordDataQuanta(dataQuanta: DataQuanta[Record]) {

  implicit def planBuilder: PlanBuilder = dataQuanta.planBuilder

  /**
    * Feed this instance into a [[MapOperator]] with a [[ProjectionDescriptor]].
    *
    * @param fieldNames names of the fields to be projected
    * @param udfCpuLoad optional [[LoadEstimator]] for the CPU consumption of the `udf`
    * @param udfRamLoad optional [[LoadEstimator]] for the RAM consumption of the `udf`
    * @return a new instance representing the [[MapOperator]]'s output
    */
  def projectRecords(fieldNames: Seq[String],
                     udfCpuLoad: LoadEstimator = null,
                     udfRamLoad: LoadEstimator = null): DataQuanta[Record] = {
    val mapOperator = MapOperator.createProjection(
      dataQuanta.output.getType.getDataUnitType.asInstanceOf[RecordType],
      fieldNames: _*
    )
    dataQuanta.connectTo(mapOperator, 0)
    wrap[Record](mapOperator)
  }

}
