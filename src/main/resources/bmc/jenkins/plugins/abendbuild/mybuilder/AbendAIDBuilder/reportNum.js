(function() {
    
    Behaviour.specify("select.my-api-dropdown", "my-api-dropdown-behavior", 0, function(dropdown) {
        

        var rowAbend = dropdown.closest('.jenkins-form-item') ;
        var parentBlock = rowAbend ? rowAbend.parentNode : document;
        var targetAbend = parentBlock.querySelector(".my-cond-reportnum");


        function togglereportNum() {
            if (dropdown.value === "report") {

                targetAbend.style.display = ""; 
            } else {

                targetAbend.style.display = "none";
            }
        }

        dropdown.addEventListener("change", togglereportNum);

      
        togglereportNum();
    });
})();
