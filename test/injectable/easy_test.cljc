(ns injectable.easy-test
  (:require [injectable.easy :as sut]
            #?(:clj [clojure.test :as t :refer [deftest testing is]]
               :cljs [cljs.test :as t :include-macros true]))
  (:import [clojure.lang ExceptionInfo]))

(deftest easy-only-syms
  (testing "Simple sums"
    (let [f (-> (sut/compile-fn-spec [+])
                (first))]
      (is (= 0 (f))))

    (let [f (-> (sut/compile-fn-spec [+ 1])
                (first))]
      (is (= 1 (f))))

    (let [f (-> (sut/compile-fn-spec [+ 1 2])
                (first))]
      (is (= 3 (f)))))

  (testing "Arities"
    (let [f (-> (sut/compile-fn-spec [(fn [a b] (/ a b)) 2 1])
                (first))]
      (is (= 2 (f))))

    (let [f (-> (sut/compile-fn-spec [(fn [a b] (/ a b)) 1])
                (first))]
      (is (thrown? clojure.lang.ArityException (f))))

    (let [f (-> (sut/compile-fn-spec [(fn [a b] (/ a b)) 1 2 3])
                (first))]
      (is (thrown? clojure.lang.ArityException (f))))))

(deftest easy-only-kws
  (testing "Simple sums"
    (let [f (-> (sut/compile-fn-spec [+ :a])
                (first))]
      (is (= 1 (f 1))))

    (let [f (-> (sut/compile-fn-spec [+ :a :b])
                (first))]
      (is (= 3 (f 1 2))))

    (let [f (-> (sut/compile-fn-spec [+ :a :b :c])
                (first))]
      (is (= 3 (f 1 1 1)))))

  (testing "Arities"
    (let [f (-> (sut/compile-fn-spec [(fn [a b] (/ a b)) :a :b])
                (first))]
      (is (= 2 (f 2 1)))
      (is (thrown? clojure.lang.ArityException (f 1)))
      (is (thrown? clojure.lang.ArityException (f 1 2 3))))))

(deftest easy-only-vars
  (testing "Simple sums"
    (let [f (-> (sut/compile-fn-spec [+ '?])
                (first))]
      (is (= 1 ((f) 1))))

    (let [f (-> (sut/compile-fn-spec [+ '? '?])
                (first))]
      (is (= 3 ((f) 1 2))))

    (let [f (-> (sut/compile-fn-spec [+ '? '? '?])
                (first))]
      (is (= 3 ((f) 1 1 1)))))

  (testing "Arities"
    (let [f (-> (sut/compile-fn-spec [(fn [a b] (/ a b)) '? '?])
                (first))]
      (is (= 2 ((f) 2 1)))
      (is (thrown? clojure.lang.ArityException ((f) 1)))
      (is (thrown? clojure.lang.ArityException ((f) 1 2 3))))))

(deftest spec-args-perm
  (let [fun (fn [args]
              (let [
                    {:keys
                     [syms
                      kws
                      vars]} (sut/spec-args-perms args)]
                (-> (vec (concat syms kws vars))
                    (sut/proj args))))]
    (testing "Permutations args"
      (is (= [3 :a '?] (fun [:a '? 3])))
      (is (= [3 :a '?] (fun [:a 3 '?])))
      (is (= [3 :a '?] (fun ['? 3 :a])))
      (is (= [3 :a '?] (fun ['? :a 3])))
      (is (= [3 :a '?] (fun [3 :a '?])))
      (is (= [3 :a '?] (fun [3 '? :a])))
      (is (= [3 4 5 6 :a :b :c '? '?] (fun [:a '? 3 :b 4 5 :c '? 6])))
      (is (= [3 4 5 6 :a :b :c '? '?] (fun [:a 3 :b '? 4 5 :c '? 6])))
      (is (= [3 4 5 6 :a :b :c '? '?] (fun [:a :b '? 3 '? 4 5 :c 6]))))))

(deftest easy-simple-mixed-cases-1
  (let [fun (fn [& args]
              (reduce #(+ (* 10 %1) %2) args))
        res (fn [argv] (-> (sut/compile-fn-spec (into [fun] argv))
                           (first)))]
    (testing "Permutations"
      (is (= 12 (as-> (res [:a 2]) f
                  (f 1)))) ;; FIXME: why do we need the extra pars?
      (is (= 21 (as-> (res [2 :a]) f
                  (f 1))))

      (is (= 12 (as-> (res ['? 2]) f
                  ((f) 1))))
      (is (= 21 (as-> (res [2 '?]) f
                  ((f) 1))))

      (is (= 231 (as-> (res ['? 3 :a]) f
                   (f 1)
                   (f 2))))
      (is (= 321 (as-> (res [3 '? :a]) f
                   (f 1)
                   (f 2)))))))

(deftest easy-simple-mixed-cases-2
  (let [fun (fn [a b c]
              (+ (* a 100)
                 (* b 10)
                 (* c 1)))
        res (fn [args] (-> (sut/compile-fn-spec (into [fun] args))
                           (first)
                           (as-> f (f 1) (f 2))))]
    (testing "Permutations"
      (is (= 123 (res [:a '? 3])))
      (is (= 132 (res [:a 3 '?])))
      (is (= 231 (res ['? 3 :a])))
      (is (= 213 (res ['? :a 3])))
      (is (= 312 (res [3 :a '?])))
      (is (= 321 (res [3 '? :a]))))))

(deftest easy-simple-mixed-cases
  (let [fun (fn [a b c d]
              (+ (* a 1000)
                 (* b 100)
                 (* c 10)
                 (* d 1)))
        res (fn [args] (-> (sut/compile-fn-spec (into [fun] args))
                           (first)
                           (as-> f (f 1) (f 2))))]
    (testing "Permutations"
      (is (= 1234 (res [:a '? 3 4])))
      (is (= 1243 (res [:a '? 4 3])))
      (is (= 1324 (res [:a 3 '? 4])))
      (is (= 1342 (res [:a 3 4 '?])))
      (is (= 1423 (res [:a 4 '? 3])))
      (is (= 1432 (res [:a 4 3 '?])))

      (is (= 2134 (res ['? :a 3 4])))
      (is (= 2143 (res ['? :a 4 3])))
      (is (= 2314 (res ['? 3 :a 4])))
      (is (= 2341 (res ['? 3 4 :a])))
      (is (= 2413 (res ['? 4 :a 3])))
      (is (= 2431 (res ['? 4 3 :a])))

      (is (= 3421 (res [3 4 '? :a])))
      (is (= 3412 (res [3 4 :a '?])))
      (is (= 3124 (res [3 :a '? 4])))
      (is (= 3142 (res [3 :a 4 '?])))
      (is (= 3214 (res [3 '? :a 4])))
      (is (= 3241 (res [3 '? 4 :a]))))))

(deftest partial-funs
  (testing "Regression: check that specs with '? return functions"
    (let [a [str '? "-" '?]
          c (first (sut/compile-fn-spec a))]
      (is (= "1-2" ((c) 1 2))))))

(deftest reg-extra-function-call
  (testing "In some cases, a function is returned instead of a value"
    (let [ret (-> (sut/compile-fn-spec [+ :a :b])
                  (first)
                  (as-> f (f 1 2)))]
      (is (= 3 ret)))))
