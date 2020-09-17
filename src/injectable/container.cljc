(ns injectable.container
  "An IOC container that is able to build a set of beans out of a definition. Supports constructor
   and 'setter' injection, through 'mutators'. A 'mutator' is a function that depends on the bean
   itself and, presumably, modifies it 'in place' when invoked.
   Constructors are functions that only depend on other beans and are expected to return a built object
   for the key."
  (:require [clojure.spec.alpha :as s]))

(s/def ::key keyword?)
(s/def ::constructor (:fun (s/cat :fun fn? :deps (s/* keyword?))))
(s/def ::mutator (s/cat :fun fn? :deps (s/* keyword?)))
(s/def ::mutators (s/coll-of ::mutator))
(s/def ::bean any?)
(s/def ::bean-def (s/keys :req-un [::constructor]
                          :opt-un [::mutators ::bean]))
(s/def ::container (s/map-of ::key ::bean-def))

(defn- bean-constructor [container-def key]
  (-> container-def key :constructor))

(defn- ensure-bean-built [cont key & [parents]]
  (when (some #(= key %) parents)
    (throw (ex-info (str "Circular dependencies: " key " depends on itself through " (rest parents))
                    {:cause "Circular dependencies" :data parents})))
  (cond
    (-> cont key :bean) cont ;; Bean already built
    (not (key cont))    (throw (ex-info (str "No definition found for bean " key)
                                        {:cause "Not bean definition" :data key}))
    :else
    (let [bean-def (key cont)
          [fun & bean-deps] (:constructor bean-def)
          ret (reduce #(ensure-bean-built %1 %2 (conj (or parents []) key))
                      cont
                      bean-deps)
          built-bean-deps (map #(-> ret % :bean)
                               bean-deps)]
      (assoc-in ret [key :bean] (apply fun built-bean-deps)))))

(defn- apply-mutator! [cont mut]
  (let [[fun & deps] mut
         ret (reduce ensure-bean-built cont deps)
         built-deps (map #(-> ret % :bean) deps)]
    (apply fun built-deps)))

(defn- apply-mutators-key! [cont key]
  (reduce apply-mutator! cont (-> cont key :mutators)))

(defn- apply-mutators! [cont]
  (doall (for [k (keys cont)] (apply-mutators-key! cont k))) cont)

(defn- build-beans-in-container [cont]
  (-> (reduce ensure-bean-built cont (keys cont))
      (apply-mutators!)))

(defn create
  "Given a container definition (list of beans conforming to :pluggable.ioc-container/container-definition)
   returns a map of constructed beans of the form {key bean}.
   It detects circular dependencies and duplicate bean definitions or constructors"
  [container]
  (as-> (build-beans-in-container container) it
    (into {} (for [[k v] it] [k (:bean v)]))))
