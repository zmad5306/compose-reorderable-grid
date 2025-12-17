plugins {
  id("com.gradleup.nmcp.aggregation")
}

group = "dev.zachmaddox.compose"
version = "1.1.0"

nmcpAggregation {
  centralPortal {
    username = System.getenv("OSSRH_USERNAME")
      ?: error("Missing OSSRH_USERNAME (Central Portal token username)")
    password = System.getenv("OSSRH_PASSWORD")
      ?: error("Missing OSSRH_PASSWORD (Central Portal token password)")

    publishingType = "AUTOMATIC"
  }
}

dependencies {
  nmcpAggregation(project(":compose-reorderable-grid"))
}
