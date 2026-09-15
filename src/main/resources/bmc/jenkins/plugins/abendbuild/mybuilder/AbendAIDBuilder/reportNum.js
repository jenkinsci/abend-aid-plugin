document.addEventListener("DOMContentLoaded", function() {
    
    var dropdown = document.getElementById('apiDropdown');
    
    
    var reportNumtar = document.querySelector('.cond-reportNum');

    if (!dropdown || !reportNumtar) return;

    function togglereportNum() {
        
        if (dropdown.value === 'report') {
            reportNumtar.style.display = ''; 
        } else {
            reportNumtar.style.display = 'none'; 
        }
    }

  
    dropdown.addEventListener('change', togglereportNum);

   
    togglereportNum();
});
