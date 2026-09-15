document.addEventListener("DOMContentLoaded", function() {
    
    var dropdown = document.getElementById('apiDropdown');
    
    
    var targetContainer = document.querySelector('.cond-reportNum');

    if (!dropdown || !targetContainer) return;

    function toggleFields() {
        
        if (dropdown.value === 'report') {
            targetContainer.style.display = ''; 
        } else {
            targetContainer.style.display = 'none'; 
        }
    }

  
    dropdown.addEventListener('change', toggleFields);

   
    toggleFields();
});
