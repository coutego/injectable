(ns injectable.core-test
  (:require [injectable.core :as sut]
            [clojure.spec.alpha :as s]
            #?(:clj [clojure.test :as t :refer [deftest testing is]]
               :cljs [cljs.test :as t :include-macros true :refer [deftest testing is]]))
  (:import [clojure.lang ExceptionInfo]))

(deftest easy-basic-test
  (testing "Basic easy tests"
    (let [ci  {:a [+ :b :c]
               :b 1
               :c 2}
          sys (sut/create-container ci)]
      (is (= 3 (:a sys))))

    (let [ci  {:a [+ :b :c]
               :b 1
               :c 2}
          c2  {:c 5}
          cf  (merge ci c2)
          sys (sut/create-container cf)]
      (is (= 6 (:a sys))))))

(deftest easy-var-test
  (testing "Simple function bean reference"
    (let [ci  {:a [:s :b :c]
               :b 1
               :c 2
               :d 10
               :s +}
          sys (sut/create-container ci)]
      (is (= 10 (:d sys)))
      (is (= 3 (:a sys)))))

  (testing "Compound function bean reference"
    (let [ci  {:a         [:dash-join :b :c]
               :dash-join [:str '? "-" '?]
               :str       str
               :b         1
               :c         2}
          sys (sut/create-container ci)]
      (is (= "1-2" (:a sys)))))

  (testing "Variadic fn as bean ref"
    (let [ci  {:a         [:dash-join :b :c]
               :dash-join [str '? "-" '?]
               :b         1
               :c         2}
          sys (sut/create-container ci)]
      (is (= "1-2" (:a sys))))))

(deftest easy-fun-shortcut
  (testing "Basic fun shortcut"
    (let [sum (fn [a b] (+ a b))
          ci  {:sum [sum 1 '?]
               :foo [:sum 2]}
          sys (sut/create-container ci)]
      (is (= 3 (:foo sys))))))

(deftest aliases
  (testing "Alias syntax is supported"
    (let [ci  {:a [+ 1 2]
               :b :a
               :c [+ :b 1]}
          sys (sut/create-container ci)]
      (is (= 3 (:a sys)))
      (is (= 4 (:c sys))))))

(deftest inner-beans
  (testing "Basic inner bean gets processed correctly"
    (let [ci  {:b1 [+ 2 [:=bean> [+ 1 1]]]}
          res (sut/create-container ci)]
      (is (= 4 (:b1 res)))))

  (testing "Crossed inner beans get processed correctly"
    (let [ci  {:b1 [+ 1 [:=bean> [+ :b2 1]]]
               :b2 1
               :b3 [+ :b1 1]}
          res (sut/create-container ci)]
      (is (= 3 (:b1 res)))
      (is (= 1 (:b2 res)))
      (is (= 4 (:b3 res)))))

  (testing "Nested inner beans are supported"
    (let [ci  {:b1 [+ 1 [:=bean> [+ :b2 [:=bean> [+ 0 :b2]]]]]
               :b2 1}
          res (sut/create-container ci)]
      (is (= 3 (:b1 res)))
      (is (= 1 (:b2 res))))))

(deftest valid-keys
  (testing "Keys starting with ':=' are not allowed"
    (let [ci {:b1 1
              :=> 1}]
      (is (thrown? ExceptionInfo (sut/create-container ci))))

    (let [ci {:b1 1
              :=b2 1}]
      (is (thrown? ExceptionInfo (sut/create-container ci))))))


(deftest mutators
  (testing "Mutators are called"
    (let [ci {:b1 1 ;
              :b2 {:constructor [atom 2]
                   :mutators    [[(fn [a v] (reset! a v)) :b2 :b1]]}}

          res (sut/create-container ci)]
      (is (= 1 (:b1 res)))
      (is (= 1 @(:b2 res)))))

  (testing "Mutual dependency through setter injection"
    (let [fa  (fn []  {:message "Hi" :from-b (atom "Mutable")})
          fas (fn [a b] (reset! (-> a :from-b) (-> b :computed)))
          fb  (fn [a] {:message  (:message a)
                       :computed (str "B: " (:message a))})
          ci  {:a {:constructor [fa]
                   :mutators    [[fas :a :b]]}
               :b {:constructor [fb :a]}}

          cs (sut/create-container ci)]
      (is (= "B: Hi" @(-> cs :a :from-b))))))
