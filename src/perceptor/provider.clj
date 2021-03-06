(ns perceptor.provider
  (:require
   [rn.clorine.pool   :as pool]
   [clj-etl-utils.log :as log])
  (:use
   [clj-etl-utils.lang-utils :only [raise aprog1]])
  (:import
   [java.util Properties]
   [com.espertech.esper.client
    EPServiceProviderManager]))

(defonce provider-registry (ref {}))

(defn register-provider [name init-fn]
  (dosync
   (pool/register-pool
    name
    (pool/make-factory
     {:make-fn
      (fn [pool-impl]
        (aprog1 (EPServiceProviderManager/getDefaultProvider)
          (init-fn it)))}))
   (alter provider-registry
          assoc
          name
          {:name    name
           :init-fn init-fn})))


(defn unregister-provider [name]
  (dosync (alter provider-registry dissoc name)))

(defn get-registered-provider [name]
  (get @provider-registry name))


(def ^{:dynamic true} *provider* :no-provider)

(defn make-provider [provider-name]
  (let [cfg      (get-registered-provider provider-name)
        provider (EPServiceProviderManager/getProvider (name provider-name))]
    (binding [*provider* provider]
      ((get cfg :init-fn) provider))
    provider))

(defn destroy-named-provider [provider-name]
  (.destroy (EPServiceProviderManager/getProvider (name provider-name))))

(defn get-provider [name]
  (.borrowObject (:pool (get @pool/*registry* name))))

(defn return-provider [name provider]
  (.returnObject (:pool (get @pool/*registry* name)) provider))


(defn with-provider* [name f]
  (pool/with-instance [instance name]
    (binding [*provider* instance]
      (f instance))))

(defmacro with-provider [[vname inst-name] & body]
  `(with-provider* ~inst-name (fn [~vname] ~@body)))

(defn map->properties* [m]
  (reduce #(do
             (.put ^java.util.Map %1 (str %2) (str (get m %2)))
             %1)
          (Properties.)
          (keys m)))

(defn map->properties [& pairs]
  (map->properties* (apply hash-map pairs)))

(defn declare-type* [^String name props]
  (.addEventType ^com.espertech.esper.client.ConfigurationOperations (.getConfiguration ^com.espertech.esper.client.EPAdministrator (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*))
                 name
                 ^Properties (map->properties* props)))

(defn declare-type [^String name & pairs]
  (declare-type* name (apply map->properties pairs)))

(defn remove-type [name]
  (.removeEventType (.getConfiguration (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*)) name true))

(defn compile-statement [stmt]
  (.createEPL (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*)
              stmt))

(defn statement-names []
  (vec (.getStatementNames (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*))))


(defn get-statement [name]
  (.getStatement (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*) name))


(defn stop-all-statements []
  (.stopAllStatements (.getEPAdministrator ^com.espertech.esper.client.EPServiceProvider *provider*)))

(def ^{:dynamic true} *new-events* :new-events-must-be-bound)
(def ^{:dynamic true} *old-events* :old-events-must-be-bound)


(defn make-listener* [f]
  (proxy [com.espertech.esper.client.UpdateListener]
      []
    (update [new-events old-events]
            (binding [*new-events* new-events
                      *old-events* old-events]
              (f))
            )))

(defmacro make-listener [& body]
  `(make-listener* (fn [] ~@body)))

(defn stmt-and-handler* [epl f]
  (let [stmt     (compile-statement epl)
        listener (make-listener* f)
        res      {:epl     epl
                  :stmt    stmt
                  :handler listener}]
    (.removeAllListeners ^com.espertech.esper.client.EPStatement stmt)
    (.addListener ^com.espertech.esper.client.EPStatement stmt ^com.espertech.esper.client.UpdateListener listener)
    res))

(defmacro stmt-and-handler [epl & body]
  `(stmt-and-handler*
    ~epl
    (fn [] ~@body)))

(defonce listeners (atom []))

(defonce registered-listeners (atom {}))

(defn register-listener [name epl f]
  (swap! registered-listeners assoc name {:name name :epl epl :impl f}))

(defn start-listener [name]
  (let [listener (get @registered-listeners name)
        epl      (:epl listener)
        stmt     (compile-statement epl)
        lstnr    (make-listener* (:impl listener))]
    (.addListener ^com.espertech.esper.client.EPStatement stmt ^com.espertech.esper.client.UpdateListener lstnr)
    (swap! listeners conj {:name     name
                             :epl      epl
                             :stmt     stmt
                             :listener lstnr})))

(defn stop-listeners []
  (doseq [listener @listeners]
    (.destroy ^com.espertech.esper.client.EPStatement (get listener :stmt)))
  (reset! listeners []))

(defn emit-event
  ([^String type k v & attrs]
     (emit-event type (apply hash-map k v attrs)))
  ([^String type ^java.util.Map attrs]
     (.sendEvent ^com.espertech.esper.client.EPRuntime (.getEPRuntime ^com.espertech.esper.client.EPServiceProvider *provider*) attrs type)))

(defn immediate-query [^String epl]
  (vec
   (.getArray
    (.execute
     (.prepareQuery ^com.espertech.esper.client.EPRuntime (.getEPRuntime ^com.espertech.esper.client.EPServiceProvider *provider*)
                    epl)))))

(comment
  (get-registered-provider :test)
  (unregister-provider :test)

  (def *p* (make-provider :test))

  (log/load-log4j-file "dev-resources/log4j.properties")
  (register-provider :test
                     (fn [p]
                       (log/infof "initializing provider: %s" p)
                       (.initialize p)))

  (with-provider [p :test]
    (log/infof "use the provider: %s" p))

  )