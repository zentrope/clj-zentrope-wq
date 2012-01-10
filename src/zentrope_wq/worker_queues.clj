(ns zentrope-wq.worker-queues
  (:import [java.util.concurrent ArrayBlockingQueue TimeUnit])
  (:require [clojure.tools.logging :as log]))

;; DEV-NOTE: All this thread management stuff needs to be re-implemented
;; using all that util.Executors stuff.

(def ^:private queues (atom {}))

(defn- break?
  [q]
  (when (= (deref (:lock q) 100 :keep-going) :exit)
    (throw (InterruptedException. "break"))))

(defn- poll
  [q]
  ;; Poll can throw an interrupted exception, which is okay.
  (let [value (.poll (:queue q) (:wait q) (TimeUnit/SECONDS))]
    (break? q)
    value))

(defn- delegate
  [q value]
  (when-let [func (:delegate q)]
    (func value)))

(defn- poll-loop
  [q]
  ;; This is getting ugly. Slingshot would probably fix
  ;; me up nicely.
  (try
    (loop []
      (when-let [value (poll q)]
        (try
          (delegate q value)
          (catch Throwable t
            (if (instance? InterruptedException t)
              (throw t)
              (if-let [err-fn (:error-handler q)]
                (err-fn (:qname q) value t)
                (log/error (:qname q) :value value :exception t))))))
      (recur))
    (catch Throwable t
      (log/debug (:qname q) t))))

(defn- mk-worker
  [q index]
  (let [thread-name (str (name  (:qname q)) ".worker." index)]
    (log/debug "creating worker" thread-name)
    (doto (Thread. (fn [] (poll-loop q)))
      (.setName thread-name)
      (.start))))

(defn- mk-workers
  [q]
  (let [workers (for [x (range (:num-workers q))] (mk-worker q x))]
    (assoc q :workers (doall workers))))

(defn- mk-queue
  [qname handler num-workers qsize error-handler]
  (mk-workers {:queue (ArrayBlockingQueue. qsize)
               :qname qname
               :wait 1
               :lock (promise)
               :size qsize
               :num-workers num-workers
               :error-handler error-handler
               :delegate handler
               :workers nil}))

;; ----------------------------------------------------------------------------
;; Public
;; ----------------------------------------------------------------------------

(defn put
  "Puts the VALUE to the named queue and blocks if the queue is full."
  [queue-name value]
  (when-let [thread (queue-name @queues)]
    (when-let [q (:queue thread)]
      (.put q value))))

(defn stop
  "Empty the queue and destroy all the workers."
  [queue-name]
  (if-let [q (queue-name @queues)]
    (do
      (deliver (:lock q) :exit)
      (.clear (:queue q))
      (swap! queues dissoc queue-name)
      :stopped)
    :queue-not-found))

(defn start
  "Create a queue, spawn num-workers worker-fns to eat from the queue. Options
are :qsize (default 1000) :error-handler (default nil,(fn [qname value exception]...))."
  [queue-name num-workers worker-fn & {:keys [qsize      error-handler]
                                       :or   {qsize 1000 error-handler nil}}]
  (let [q (mk-queue queue-name worker-fn num-workers qsize error-handler)]
    (swap! queues assoc queue-name q)
    queue-name))
