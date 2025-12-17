plugins {
    id("com.gradleup.nmcp.aggregation") version "1.3.0"
}

group = "dev.zachmaddox.compose"
version = "1.1.0"

nmcpAggregation {
    centralPortal {
        username = System.getenv("OSSRH_USERNAME")
        password = System.getenv("OSSRH_PASSWORD")
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    nmcpAggregation(project(":compose-reorderable-grid"))
}
