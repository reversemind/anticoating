(ns com.test.go2)

(defn -main [args]
  (type args)
    (println (first args)))


;
;(ns com.test.go2)
;
;
;(defn -main [& args]
;  (let [in (slurp *in*)]
;    (println in)))
