package com.github.mmvpm
package matching

import matching.Matchable._
import matching.util.HostUtils._
import model._

object Instances {

  // name

  val matchNames: Matchable1[ServiceName] =
    byEquals[String].lift1(_.name)

  // fqn

  val matchFqns: Matchable1[ServiceFqn] =
    byEquals[String].lift1(_.fqn)

  // ip

  val matchIps: Matchable1[Ip] =
    byEquals[String].lift1(_.ip)

  val matchIpConfig: Matchable[Ip, KeyValuePair] =
    matchIps.liftR(kv => Ip(kv.value))

  // host

  val matchHosts: Matchable1[Host] =
    byEquals[String].lift1(host => normalizeHost(host.host))

  val matchHostConfig: Matchable[Host, KeyValuePair] =
    matchHosts.liftR(kv => Host(kv.value))

  // service

  val matchServices: Matchable[Service, Service] = {
    val byIp = matchIps.options.lift1[Service](_.ip)
    val byFqn = matchFqns.options.lift1[Service](_.fqn)
    val byHost = matchHosts.options.lift1[Service](_.host)
    any(byFqn, byIp, byHost)
  }

  val byShivaHost: Matchable[Service, KeyValuePair] =
    (service: Service, config: KeyValuePair) =>
      config.value match {
        case shivaHostRegex(name, interface) =>
          service.name.exists(_.name == name) && service.interfaces.flatMap(_.name).contains(interface)
        case shivaUrlRegex(name, interface) =>
          service.name.exists(_.name == name) && service.interfaces.flatMap(_.name).contains(interface)
        case _ =>
          false
      }

  val byInternalHost: Matchable[Service, KeyValuePair] =
    (service: Service, config: KeyValuePair) => {
      val host = normalizeHost(config.value)
      possibleHostPrefixes(service).exists { prefix =>
        host.startsWith(prefix) && host.endsWith(InternalHostSuffix)
      }
    }

  val matchServiceConfig: Matchable[Service, KeyValuePair] = {
    val byIp = matchIpConfig.optionL.liftL[Service](_.ip)
    val byHost = matchHostConfig.optionL.liftL[Service](_.host)
    any(byIp, byHost, byShivaHost, byInternalHost)
  }
}