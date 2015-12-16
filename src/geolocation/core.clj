(ns geolocation.core)

(def ^:private MIN_LAT (Math/toRadians -90)) ;; -PI/2
(def ^:private MAX_LAT (Math/toRadians 90))  ;;  PI/2
(def ^:private MIN_LON (Math/toRadians -180));;  -PI
(def ^:private MAX_LON (Math/toRadians 180)) ;;  PI

(def EARTH-RADIUS 6371) ;; in kms

(defprotocol DegreeRadiansConversion
  (to-degrees [this])
  (to-radians [this]))

(defrecord GeoLocation [^Double lat, ^Double lon]
  DegreeRadiansConversion
  (to-degrees [this]
    [(Math/toDegrees (:lat this)) (Math/toDegrees (:lon this))])
  (to-radians [this]
    [(:lat this) (:lon this)]))


(defn- validate [{:keys [lat lon] :as arg}]
  (if (or (> lat MAX_LAT)
          (< lat MIN_LAT)
          (> lon MAX_LON)
          (< lon MIN_LON))
    (throw (ex-info "Values outside range" {})) arg))

(defn from-radians [lat lon]
  (validate
   (->GeoLocation lat lon)))

(defn from-degrees [lat lon]
  (validate
   (->GeoLocation (Math/toRadians lat) (Math/toRadians lon))))

(defn distance
  ([from to] (distance from to EARTH-RADIUS))
  ([from to radius]
            (* radius
               (Math/acos (+ (* (Math/sin (:lat from))
                                (Math/sin (:lat to)))
                             (* (* (Math/cos (:lat from))
                                   (Math/cos (:lat to)))
                                (Math/cos (- (:lon from)
                                             (:lon to)))))))))

(defn- calc-min-max [lat lon dist min-lat max-lat]
  (if (and (> min-lat MIN_LAT) (< max-lat MAX_LAT))
    (let [delta-lon (Math/asin (/ (Math/sin dist) (Math/cos lat)))
          min-lon (- lon delta-lon)
          min-lon (if (< min-lon MIN_LON)
                    (+ min-lon (* 2 Math/PI)) min-lon)
          max-lon (+ lon delta-lon)
          max-lon (if (> max-lon MAX_LON)
                    (- max-lon (* 2 Math/PI)) max-lon)]
      [min-lat max-lat min-lon max-lon])
    [(max min-lat MIN_LAT)
     (min max-lat MAX_LAT)
     MIN_LON
     MAX_LON]))

(defn bounds
  ([geo-location distance] (bounds geo-location distance EARTH-RADIUS))
  ([geo-location distance radius]

     (when (or (neg? radius)
               (neg? distance))
       (throw (ex-info "Values cannot be negative"
                       {:geo-location geo-location
                        :distance distance
                        :radius radius})))

     (let [[lat lon] (to-radians geo-location)
           dist (/ distance radius)
           min-lat (- lat dist)
           max-lat (+ lat dist)

           [min-lat max-lat min-lon max-lon]
           (calc-min-max lat lon dist min-lat max-lat)]
       [(->GeoLocation min-lat min-lon)
        (->GeoLocation max-lat max-lon)])))


(def LAX (from-degrees 33.9415933,-118.410724))
(def SFO (from-degrees 37.6213171,-122.3811494))
(def JFK (from-radians 0.6696179670832446, -2.0856033047375764))
(def JFK-2 (from-degrees 40.6413151,-73.7803331))
(def JFK-3 (from-radians 0.7093247608354885,-1.2877097358131546))

(println (distance LAX SFO))
(println (distance SFO JFK-3))
(println (distance LAX JFK-3))
(println (map to-degrees  (bounds SFO 100)))

(def R (* 200 EARTH-RADIUS))
(println (distance LAX SFO R))

(println (to-radians JFK-2))

(pr (mapv to-degrees (bounds JFK-3 10 R)))
