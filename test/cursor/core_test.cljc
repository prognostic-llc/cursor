#?(:clj  (ns cursor.core-test
           (:require [clojure.test :refer [deftest testing is]]
                     [cursor.core :refer [cursor]]
                     [cursor.root-at :refer [root-at]]))
   :cljs (ns cursor.core-test
           (:require-macros [cljs.test :refer [deftest testing is]])
           (:require [cljs.test]
                     [cursor.core :refer [cursor]]
                     [cursor.root-at :refer [root-at]])))


(deftest test-root-at []
  (is (= (let [f (root-at [:a :b] inc (constantly false) nil)]
           (f {:a {:b 0}}))
         {:a {:b 1}})))


(deftest test-root-at-no-path []
  (is (= (let [inc' (root-at [] inc (constantly false) nil)]
           (inc' 0))
         1)))


(deftest test-value []
  (let [store (atom {:a 0})
        cur (cursor store)]
    (is (= @cur {:a 0}))))


(deftest test-swap []
  (let [store (atom 0)
        cur (cursor store)]
    (swap! cur inc)
    (is (= @store 1))))


(deftest test-refine []
  (let [store (atom {:a 0})
        cur (cursor store)]
    (is (= @(cur [:a]) 0))))

(deftest refine-not-found []
  (let [store (atom {:a 0})
        cur (cursor store)]
    (is (= @(cur [:b] []) []))))


(def initialMap {:a {:b 1}, :xs [1 2 3]})

(deftest identity-swap1 []
  (let [store (atom initialMap)
        cur (cursor store)]

    (-> cur (swap! identity))
    (is (= @store initialMap))))


(deftest test-default-refines []
  (let [store (atom initialMap)
        cur (cursor store)]
    (is (= @((cur [:c] {:d 10}) [:d]) 10))
    (let [dcur ((cur [:c] {:d 10}) [:d])]
      (swap! dcur identity)
      (is (= @dcur 10))
      (swap! dcur (constantly 11))
      (is (= @dcur 10))
      (is (= @((cursor store) [:c :d]) 11)))))

(deftest test-default-refines-with-valid-nil []
  (let [initial {:a {:b nil}}
        store (atom initial)
        cur (cursor store)
        bcur (cur [:a :b] 42)]                              ; nil is what we want, don't return not found
    (is (= @bcur nil))
    (swap! bcur (fn [v]
                  ; when we swap, we expext the valid value of nil
                  (assert (nil? v))
                  v))
    (is (= nil (get-in @store [:a :b]))))

  (let [initial {:a {:b nil}}
        store (atom initial)
        cur (cursor store)
        bcur (cur [:a :b] 42 nil?)]                         ; not-found is what we want, don't return nil
    (is (= @bcur 42))
    (swap! bcur inc)
    (is (= 43 (get-in @store [:a :b])))))


(deftest test-swap-against-notfound []
  (let [initial {}
        store (atom initial)
        cur (cursor store)]
    (-> (cur [:a] {:b 1})
        (swap! update-in [:b] inc))
    (is (= (get-in @store [:a :b]) 2))))


(deftest swap-merge []
  (let [store (atom initialMap)
        cur (cursor store)]
    (-> (cur [:a]) (swap! merge {:z 99}))
    (is (= (get-in @store [:a :z]) 99))))


(deftest reset []
  (let [store (atom initialMap)
        cur (cursor store)]
    (is (= @store @cur))
    (swap! (cur [:a]) (constantly 42))
    (is (not= @store @cur))
    (is (= (get-in @store [:a]) 42))
    (reset! (cur [:a]) 43)
    (is (= (get-in @store [:a]) 43))))

