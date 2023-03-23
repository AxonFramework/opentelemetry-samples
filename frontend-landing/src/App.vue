<script setup lang="ts">


</script>

<template>
  <div class="container-fluid">
    <nav class="navbar navbar-expand-lg navbar-light bg-white">
      <div class="container-fluid px-3">
        <a class="navbar-brand" href="/"><img src="./assets/logo.svg" alt="" width="30" height="24">Axon Observability
          training</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent"
                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>

        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <a class="nav-link active" aria-current="page" href="#">Home</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" target="_blank" href="/service-interfaces/">Auction interface</a>
          </li>
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle" target="_blank" href="#" role="button" data-bs-toggle="dropdown"
               aria-expanded="false">
              Firestarter
            </a>
            <ul class="dropdown-menu">
              <li><a class="dropdown-item" target="_blank" href="/service-auctions/fire-starter/">Auctions</a></li>
              <li><a class="dropdown-item" target="_blank" href="/service-query/fire-starter/">Auction Query</a></li>
              <li><a class="dropdown-item" target="_blank" href="/service-object-registry/fire-starter/">Auction Object
                Registry</a></li>
              <li><a class="dropdown-item" target="_blank" href="/service-participants/fire-starter/">Participants</a>
              </li>
            </ul>
          </li>
          <li class="nav-item">
            <a class="nav-link" target="_blank" href="/axon-server/">Axon Server</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" target="_blank" href="/grafana/">Grafana</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" target="_blank" href="/jaeger/">Jaeger</a>
          </li>
        </ul>

      </div>
    </nav>

    <div class="bg-white mt-4 p-4">
      <div class="row">
        <div class="col-md-5">
          <h1 class="text-secondary">Welcome!</h1>
          <p>You have arrived at the Axon Observability Training exercises application. It's time to put all that theory
            into practice and make sense of all metrics and data available to you.</p>

          <p>
            During this training you will observe a high-concurrency and scalable demo, so you can push Axon Framework
            and Server to its limits in a safe environment. This will give you a lot of experience, gained in a safe
            environments.
            While doing so, please feel free to ask questions to your trainer.
          </p>

          <p>The top bar contains many useful links that you can click at any time. They will open a new window.</p>
          <ul>
            <li><b>Auction Interface: </b> Here you can observe the high-concurrency demo in action. You can also scale
              its configuration there.
            </li>
            <li><b>FireStarter: </b> This section contains links to the FireStart of each server. Here you can mess with
              Axon's timings and error rate, in a true chaos way.
            </li>
            <li><b>Axon Server: </b> Access to the Axon Server dashboard. This demo runs Standard Edition to prevent the
              need for license management.
            </li>
            <li><b>Grafana: </b> Grafana can visualize metrics, host your dashboards and alert when stuff goes wrong.
              This is an important part of the training.
            </li>
            <li><b>Jaeger: </b> The Jaeger UI is used to access tracing. Feel free to poke around.</li>
          </ul>

          <div class="p-2">
            <h2>Use Case</h2>
            <p>
              The Auction house hosts participants that enter a bidding war. They all try to make the most money out of
              the items they have by selling them, and buying others.
            </p>
            <p>
              Each participant gets 3 items to start, and is free to start selling them at any time. Each participant
              listens to updates to auctions, and upon updates will decide whether they are interested and if they enter
              the bidding war. If so, each participant has an unique limit and will start bidding. Each auction ends
              after 10 seconds and the highest bid wins!
            </p>

            <p>
              When an auction ends, the item that was auctioned is transferred by the Object Registry service once the
              balances have been settled.
            </p>

            <p>You can see all this visualized in the following diagram:</p>
            <img class="p-2" src="./assets/sequence_overview.png" style="width: 100%">
            <p>You can increase the number of participants, in the Configuration tab of the Auction interface. Each
              participant additionally will mean more auctions, and more bids per auction. Scale wisely!</p>
          </div>

          <div class="p-2">
            <h2>Metric cheat sheet</h2>
            <p>TODO: Will contain the tables from the presentations</p>
          </div>
        </div>
        <div class="col-md-7 pt-5">
          <div class="accordion" id="accordionExample">
            <div class="accordion-item">
              <h2 class="accordion-header" id="headingOne">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne"
                        aria-expanded="true" aria-controls="collapseOne">
                  Exercise 1: Metrics
                </button>
              </h2>
              <div id="collapseOne" class="accordion-collapse collapse show" aria-labelledby="headingOne"
                   data-bs-parent="#accordionExample">
                <div class="accordion-body">
                  <strong>This is the first item's accordion body.</strong> It is shown by default, until the collapse
                  plugin adds the appropriate classes that we use to style each element. These classes control the
                  overall appearance, as well as the showing and hiding via CSS transitions. You can modify any of this
                  with custom CSS or overriding our default variables. It's also worth noting that just about any HTML
                  can go within the <code>.accordion-body</code>, though the transition does limit overflow.
                </div>
              </div>
            </div>
            <div class="accordion-item">
              <h2 class="accordion-header" id="headingTwo">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                        data-bs-target="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
                  Exercise 2: Alerting & Tracing
                </button>
              </h2>
              <div id="collapseTwo" class="accordion-collapse collapse" aria-labelledby="headingTwo"
                   data-bs-parent="#accordionExample">
                <div class="accordion-body">
                  <strong>This is the second item's accordion body.</strong> It is hidden by default, until the collapse
                  plugin adds the appropriate classes that we use to style each element. These classes control the
                  overall appearance, as well as the showing and hiding via CSS transitions. You can modify any of this
                  with custom CSS or overriding our default variables. It's also worth noting that just about any HTML
                  can go within the <code>.accordion-body</code>, though the transition does limit overflow.
                </div>
              </div>
            </div>
            <div class="accordion-item">
              <h2 class="accordion-header" id="headingThree">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                        data-bs-target="#collapseThree" aria-expanded="false" aria-controls="collapseThree">
                  Exercise 3: Problem Solving
                </button>
              </h2>
              <div id="collapseThree" class="accordion-collapse collapse" aria-labelledby="headingThree"
                   data-bs-parent="#accordionExample">
                <div class="accordion-body">
                  <strong>This is the third item's accordion body.</strong> It is hidden by default, until the collapse
                  plugin adds the appropriate classes that we use to style each element. These classes control the
                  overall appearance, as well as the showing and hiding via CSS transitions. You can modify any of this
                  with custom CSS or overriding our default variables. It's also worth noting that just about any HTML
                  can go within the <code>.accordion-body</code>, though the transition does limit overflow.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>


    </div>
  </div>
</template>

<style scoped>
.selling {
  color: orange;
}

.accordion-item {
  --bs-accordion-bg: transparent
}
</style>
