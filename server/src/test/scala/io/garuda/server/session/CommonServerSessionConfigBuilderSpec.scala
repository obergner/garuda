package io.garuda.server.session

import com.codahale.metrics
import io.garuda.common.builder.BuilderContext
import io.garuda.common.resources.ResourcesRegistry
import io.garuda.server.authentication.support.FixedPasswordAuthenticator
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * Created by obergner on 26.03.14.
 */
class CommonServerSessionConfigBuilderSpec extends Specification with NoTimeConversions {

  def builderContext(): BuilderContext = {
    val metricReg = new metrics.MetricRegistry
    val resourcesReg = new ResourcesRegistry(getClass.getSimpleName, 10 milliseconds, metricReg)
    BuilderContext(metricReg, resourcesReg)
  }

  "CommonServerSessionConfigBuilder" should {

    "reject null system ID" in {
      CommonServerSessionConfigBuilder(builderContext()).systemId(null) must throwAn[IllegalArgumentException]
    }

    "reject empty system ID" in {
      CommonServerSessionConfigBuilder(builderContext()).systemId("") must throwAn[IllegalArgumentException]
    }

    "reject null Authenticator" in {
      CommonServerSessionConfigBuilder(builderContext()).bindConfigBuilder().authenticator(null) must throwAn[IllegalArgumentException]
    }

    "reject 0 bind timeout (ms)" in {
      CommonServerSessionConfigBuilder(builderContext()).bindConfigBuilder().bindTimeoutMillis(0) must throwAn[IllegalArgumentException]
    }

    "reject -1000 bind timeout (ms)" in {
      CommonServerSessionConfigBuilder(builderContext()).bindConfigBuilder().bindTimeoutMillis(-1000) must throwAn[IllegalArgumentException]
    }

    "successfully create a default CommonServerSessionConfig if given no parameters" in {
      val defaultSession = CommonServerSessionConfigBuilder(builderContext()).build()

      defaultSession.systemId mustEqual CommonServerSessionConfig.DefaultSystemId
      defaultSession.bindConfig.authenticator mustEqual BindConfig.DefaultAuthenticator
      defaultSession.bindConfig.bindTimeoutMillis mustEqual BindConfig.DefaultBindTimeoutMillis
    }

    "successfully create a CommonServerSessionConfig with all options passed in" in {
      val expectedSystemId = "expectedSystemId"
      val expectedAuthenticator = FixedPasswordAuthenticator("password")
      val expectedBindTimeoutMillis = 125L
      val expectedEnquireLinkResponseTimeoutMillis = 255L
      val session = CommonServerSessionConfigBuilder(builderContext()).
        systemId(expectedSystemId).
        bindConfigBuilder().authenticator(expectedAuthenticator).
        bindTimeoutMillis(expectedBindTimeoutMillis).end().
        periodicPingConfigBuilder().
        pingResponseTimeoutMillis(expectedEnquireLinkResponseTimeoutMillis).
        build()

      session.systemId mustEqual expectedSystemId
      session.bindConfig.authenticator must_== expectedAuthenticator
      session.bindConfig.bindTimeoutMillis mustEqual expectedBindTimeoutMillis
      session.periodicPingConfig.pingResponseTimeoutMillis mustEqual expectedEnquireLinkResponseTimeoutMillis
    }
  }
}
