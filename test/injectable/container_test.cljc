(ns injectable.container-test
  (:require [injectable.container :as sut]
            [clojure.spec.alpha :as s]
            #?(:clj [clojure.test :as t :refer [deftest testing is]]
               :cljs [cljs.test :as t :include-macros true :refer [deftest testing is]]))
  (:import [clojure.lang ExceptionInfo]))

(deftest container-simple-tests
  (testing "single bean"
    (let [cons (fn [] 1)
          ci {:one {:constructor [cons]}}
          cs (sut/create ci)]
      (is (= 1 (-> cs :one)))))

  (testing "Two independent beans"
    (let [f1 (fn [] 1)
          f2 (fn [] 2)
          ci {:one {:constructor [f1]}
              :two {:constructor [f2]}}
          cs (sut/create ci)]
      (is (= 1 (-> cs :one)))
      (is (= 2 (-> cs :two)))))

  (testing "Two dependent beans"
    (let [f1 (fn [] 1)
          f2 (fn [x] x)
          ci {:one {:constructor [f1]}
              :two {:constructor [f2 :one]}}
          cs (sut/create ci)]
      (is (= 1 (-> cs :one)))
      (is (= 1 (-> cs :two)))))

  (testing "Two dependent beans, inverse order"
    (let [f1 (fn [] 1)
          f2 (fn [x] x)
          ci {:two {:constructor [f2 :one]}
              :one {:constructor [f1]}}
          cs (sut/create ci)]
      (is (= 1 (-> cs :one)))
      (is (= 1 (-> cs :two))))))

(deftest vectors
  (testing "Basic vector"
    (let [ci {:one {:constructor [vector :two]}
              :two {:constructor [(fn [] 2)]}}
          cs (sut/create ci)]
      (is (= [2] (-> cs :one))))))

(deftest circular-dependencies-test
  (testing "Three interdependent beans"
    (let [ci {:a {:constructor [identity :b]}
              :b {:constructor [identity :c]}
              :c {:constructor [identity :a]}}]
      (is (thrown? ExceptionInfo (sut/create ci))))))

(deftest bean-not-defined-test
  (testing "No beans defined"
    (let [ci {:a {:constructor [identity :b]}}]
      (is (thrown? ExceptionInfo (sut/create ci))))))

(deftest mutators
  (testing "Mutators are called"
    (let [ci  {:b1 {:constructor [(fn [] 1)]} 
               :b2 {:constructor [(fn [] (atom 2))]
                    :mutators    [[(fn [a v] (reset! a v)) :b2 :b1]]}}

          res (sut/create ci)]
      (is (= 1 (:b1 res)))
      (is (= 1 @(:b2 res)))))

  (testing "Mutual dependency through setter injection"
    (let [fa  (fn []  {:message "Hi" :from-b (atom "Mutable")})
          fas (fn [a b] (reset! (-> a :from-b) (-> b :computed)))
          fb  (fn [a] {:message (:message a)
                       :computed (str "B: " (:message a))})
          ci {:a {:constructor [fa]
                  :mutators [[fas :a :b]]}
              :b {:constructor [fb :a]}}

          cs (sut/create ci)]
      (is (= "B: Hi" @(-> cs :a :from-b))))))

