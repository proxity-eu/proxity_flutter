import Flutter
import UIKit
import CoreLocation
import ProxityKit

public class SwiftProxityFlutterPlugin: NSObject, CLLocationManagerDelegate, ProxityDelegate, FlutterPlugin {
    private var locationManager: CLLocationManager?
    private var proxityClient: ProxityKit.ProxityClient?
    private var regions: [CLBeaconRegion] = []

    static var instance: SwiftProxityFlutterPlugin!
    private var channel: FlutterMethodChannel!
    private var messagesSink: ContentStreamHandler!
    private var webhooksSink: ContentStreamHandler!

    public static func register(with registrar: FlutterPluginRegistrar) {
        instance = SwiftProxityFlutterPlugin()
        instance.channel = FlutterMethodChannel(
            name: "eu.proxity",
            binaryMessenger: registrar.messenger()
        )

        instance.messagesSink = ContentStreamHandler()
        let messages = FlutterEventChannel(
            name: "eu.proxity.messages",
            binaryMessenger: registrar.messenger()
        )
        messages.setStreamHandler(instance.messagesSink)

        instance.webhooksSink = ContentStreamHandler()
        let webhooks = FlutterEventChannel(
            name: "eu.proxity.webhooks",
            binaryMessenger: registrar.messenger()
        )
        webhooks.setStreamHandler(instance.webhooksSink)

        registrar.addMethodCallDelegate(instance, channel: instance.channel)
    }

    public func content(_ client: ProxityClient, messages: [UUID]) {
        messagesSink.sink?(messages.map { $0.uuidString })
    }

    public func content(_ client: ProxityClient, webhooks: [UUID]) {
        webhooksSink.sink?(webhooks.map { $0.uuidString })
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            guard let args = call.arguments as? [String:Any],
                  let apiKey = args["apiKey"] as? String,
                  let deviceId = args["deviceId"] as? String?
            else {
                result(false)
                return
            }

            locationManager = CLLocationManager()
            locationManager!.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
            locationManager!.delegate = self

            proxityClient = ProxityKit.ProxityClient(
                apiKey: apiKey,
                deviceId: UUID(uuidString: deviceId ?? UUID().uuidString) ?? UUID(),
                logger: { print("ProxityKit: \($0)") },
                recreateDatabase: false
            )
            proxityClient?.delegate = self
            result(proxityClient != nil);

        case "start":
            result(nil)

        case "webhooks":
            guard let args = call.arguments as? [String:Any],
                  let ids = args["ids"] as? [String],
                  let data = args["data"] as? String?
            else {
                result(nil)
                return
            }

            let uuids = ids.map { UUID(uuidString: $0)! } // TODO(yupi) sanity check
            proxityClient?.dispatchWebhooks(ids: uuids, data: data)
            result(nil)

        case "location":
            guard let location = locationManager?.location else {
                result(nil)
                return
            }

            result([
                "latitude":         location.coordinate.latitude,
                "longitude":        location.coordinate.longitude,
                "accuracy":         location.horizontalAccuracy,
                "speed":            location.speed,
                "speedAccuracy":    location.speedAccuracy,
                "altitude":         location.altitude,
                "verticalAccuracy": location.verticalAccuracy,
                "timestamp":        location.timestamp.timeIntervalSince1970,
            ])

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didRange beacons: [CLBeacon],
        satisfying beaconConstraint: CLBeaconIdentityConstraint
    ) {
        if !beacons.isEmpty {
            proxityClient?.beaconsRanged(beacons, location: manager.location)
        }
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        proxityClient?.syncRegions(for: locations.first) { regions in
            for region in self.regions {
                manager.stopRangingBeacons(satisfying: region.beaconIdentityConstraint)
                manager.stopMonitoring(for: region)
            }

            for region in regions {
                manager.startMonitoring(for: region)
            }

            self.regions = regions
        }
    }

    public func locationManager(
        _ manager: CLLocationManager,
        monitoringDidFailFor region: CLRegion?,
        withError error: Error
    ) {
        print(error)
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didStartMonitoringFor region: CLRegion
    ) {
        print("start monitoring for \(region)")
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didEnterRegion region: CLRegion
    ) {
        let region = region as! CLBeaconRegion
        manager.startRangingBeacons(satisfying: region.beaconIdentityConstraint)
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didExitRegion region: CLRegion
    ) {
        let region = region as! CLBeaconRegion
        manager.stopRangingBeacons(satisfying: region.beaconIdentityConstraint)
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didChangeAuthorization status: CLAuthorizationStatus
    ) {
        if status == .authorizedAlways || status == .authorizedWhenInUse {
            manager.startUpdatingLocation()
            return
        }

        switch status {
        case .authorizedAlways:
            print("Location: authorizedAlways")
        case .authorizedWhenInUse:
            print("Location: authorizedWhenInUse")
        case .denied:
            print("Location: denied")
        case .notDetermined:
            print("Location: not determined")
            manager.requestAlwaysAuthorization()
        case .restricted:
            print("Location: restricted")
        @unknown default:
            print("Location: unknown")
        }
    }
}

class ContentStreamHandler : NSObject, FlutterStreamHandler {
    var sink: FlutterEventSink?

    func onListen(
        withArguments arguments: Any?,
        eventSink events: @escaping FlutterEventSink
    ) -> FlutterError? {
        sink = events
        return nil
    }

    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        sink = nil
        return nil
    }
}
