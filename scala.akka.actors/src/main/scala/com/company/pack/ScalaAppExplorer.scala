package com.company.pack

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

object ScalaAppExplorer extends App {
  SpringApplication.run(classOf[ScalaAppExplorer], args: _ *)
}

@SpringBootApplication
@ComponentScan(basePackages = Array("com.company"))
class ScalaAppExplorer {

}